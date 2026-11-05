package com.oriolvc.topicman.collection.application.usecase;

import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.application.model.CreateCollectionCommand;
import com.oriolvc.topicman.collection.domain.exception.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.CollectionName;
import org.junit.jupiter.api.Test;

class CreateCollectionUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final CreateCollectionUseCase useCase = new CreateCollectionUseCase(repository, CLOCK);

    @Test
    void createsAnEmptyCollection() {
        var result = useCase.execute(new CreateCollectionCommand(new CollectionName("Orders")));

        assertThat(result.name()).isEqualTo("Orders");
        assertThat(result.templates()).isEmpty();
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(repository.findById(new CollectionId(result.id()))).isPresent();
    }

    @Test
    void rejectsATakenName() {
        useCase.execute(new CreateCollectionCommand(new CollectionName("Orders")));

        assertThatThrownBy(() -> useCase.execute(new CreateCollectionCommand(new CollectionName("Orders"))))
                .isInstanceOf(CollectionNameTakenException.class);
    }
}
