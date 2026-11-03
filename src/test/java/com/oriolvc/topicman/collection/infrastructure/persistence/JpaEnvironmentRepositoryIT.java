package com.oriolvc.topicman.collection.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.collection.domain.Environment;
import com.oriolvc.topicman.collection.domain.EnvironmentName;
import com.oriolvc.topicman.collection.domain.EnvironmentNameTakenException;
import com.oriolvc.topicman.collection.domain.VariableName;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@DataJpaTest
@TestConstructor(autowireMode = AutowireMode.ALL)
@Import({TestcontainersConfiguration.class, JpaEnvironmentRepository.class})
class JpaEnvironmentRepositoryIT {

    private static final Instant CREATED_AT = Instant.parse("2026-11-03T10:00:00.123456Z");

    private final JpaEnvironmentRepository repository;
    private final EntityManager entityManager;

    JpaEnvironmentRepositoryIT(JpaEnvironmentRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsAnEnvironmentWithItsVariablesInOrder() {
        var variables = new LinkedHashMap<VariableName, String>();
        variables.put(new VariableName("zone"), "eu-west");
        variables.put(new VariableName("app.suffix"), "");
        var environment = Environment.create(new EnvironmentName("staging"), variables, CREATED_AT);

        repository.save(environment);
        flushAndClear();

        assertThat(repository.findById(environment.id())).hasValueSatisfying(found -> {
            assertThat(found.name()).isEqualTo(new EnvironmentName("staging"));
            assertThat(found.variables())
                    .containsExactly(
                            Map.entry(new VariableName("zone"), "eu-west"),
                            Map.entry(new VariableName("app.suffix"), ""));
            assertThat(found.createdAt()).isEqualTo(CREATED_AT);
        });
    }

    @Test
    void replacesItsVariables() {
        var environment =
                Environment.create(new EnvironmentName("staging"), Map.of(new VariableName("env"), "stg"), CREATED_AT);
        repository.save(environment);
        flushAndClear();
        environment.replaceVariables(Map.of(new VariableName("region"), "eu"), CREATED_AT.plusSeconds(1));
        environment.rename(new EnvironmentName("stage"), CREATED_AT.plusSeconds(1));

        repository.save(environment);
        flushAndClear();

        assertThat(repository.findById(environment.id())).hasValueSatisfying(found -> {
            assertThat(found.name()).isEqualTo(new EnvironmentName("stage"));
            assertThat(found.variables()).containsExactly(Map.entry(new VariableName("region"), "eu"));
        });
    }

    @Test
    void listsEnvironmentsByNameAndRejectsATakenName() {
        repository.save(Environment.create(new EnvironmentName("staging"), Map.of(), CREATED_AT));
        repository.save(Environment.create(new EnvironmentName("dev"), Map.of(), CREATED_AT));
        flushAndClear();

        assertThat(repository.findAll())
                .extracting(found -> found.name().value())
                .containsExactly("dev", "staging");
        assertThatThrownBy(() -> repository.save(Environment.create(new EnvironmentName("dev"), Map.of(), CREATED_AT)))
                .isInstanceOf(EnvironmentNameTakenException.class);
    }

    @Test
    void deletesAnEnvironmentWithItsVariables() {
        var environment =
                Environment.create(new EnvironmentName("staging"), Map.of(new VariableName("env"), "stg"), CREATED_AT);
        repository.save(environment);
        flushAndClear();

        repository.deleteById(environment.id());
        flushAndClear();

        assertThat(repository.findById(environment.id())).isEmpty();
        var variables = ((Number) entityManager
                        .createNativeQuery("SELECT COUNT(*) FROM environment_variable")
                        .getSingleResult())
                .longValue();
        assertThat(variables).isZero();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
