package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.CollectionNotFoundException;
import org.junit.jupiter.api.Test;

class GetCollectionUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final GetCollectionUseCase useCase = new GetCollectionUseCase(repository);

    @Test
    void getsACollectionWithItsTemplates() {
        var created = new CreateCollectionUseCase(repository, CLOCK)
                .execute(new CreateCollectionCommand(new CollectionName("Orders")));
        var id = new CollectionId(created.id());
        new AddTemplateUseCase(repository, new FakeSupportedBrokers(), CLOCK)
                .execute(new AddTemplateCommand(id, aKafkaTemplate("Order created")));

        var result = useCase.execute(new GetCollectionCommand(id));

        assertThat(result.templates()).extracting(TemplateResult::name).containsExactly("Order created");
    }

    @Test
    void failsForAnUnknownCollection() {
        assertThatThrownBy(() -> useCase.execute(new GetCollectionCommand(CollectionId.newId())))
                .isInstanceOf(CollectionNotFoundException.class);
    }
}
