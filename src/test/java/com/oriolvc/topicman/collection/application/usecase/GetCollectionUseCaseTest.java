package com.oriolvc.topicman.collection.application.usecase;

import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.application.model.AddTemplateCommand;
import com.oriolvc.topicman.collection.application.model.CreateCollectionCommand;
import com.oriolvc.topicman.collection.application.model.GetCollectionCommand;
import com.oriolvc.topicman.collection.application.model.TemplateResult;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.CollectionName;
import com.oriolvc.topicman.collection.domain.model.TestIds;
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
        assertThatThrownBy(() -> useCase.execute(new GetCollectionCommand(TestIds.collectionId())))
                .isInstanceOf(CollectionNotFoundException.class);
    }
}
