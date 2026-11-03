package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import org.junit.jupiter.api.Test;

class ListCollectionsUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final CreateCollectionUseCase create = new CreateCollectionUseCase(repository, CLOCK);
    private final AddTemplateUseCase addTemplate =
            new AddTemplateUseCase(repository, new FakeSupportedBrokers(), CLOCK);
    private final ListCollectionsUseCase useCase = new ListCollectionsUseCase(repository);

    @Test
    void listsSummariesOrderedByName() {
        var shipping = create.execute(new CreateCollectionCommand(new CollectionName("Shipping")));
        create.execute(new CreateCollectionCommand(new CollectionName("Billing")));
        addTemplate.execute(new AddTemplateCommand(new CollectionId(shipping.id()), aKafkaTemplate("Shipped")));

        var result = useCase.execute(new ListCollectionsCommand());

        assertThat(result).extracting(CollectionSummaryResult::name).containsExactly("Billing", "Shipping");
        assertThat(result).extracting(CollectionSummaryResult::templateCount).containsExactly(0, 1);
    }
}
