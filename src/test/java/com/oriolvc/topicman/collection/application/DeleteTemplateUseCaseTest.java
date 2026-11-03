package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.TemplateId;
import com.oriolvc.topicman.collection.domain.TemplateNotFoundException;
import com.oriolvc.topicman.collection.domain.TestIds;
import org.junit.jupiter.api.Test;

class DeleteTemplateUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final DeleteTemplateUseCase useCase = new DeleteTemplateUseCase(repository, CLOCK);
    private final CollectionId collection = new CollectionId(new CreateCollectionUseCase(repository, CLOCK)
            .execute(new CreateCollectionCommand(new CollectionName("Orders")))
            .id());

    @Test
    void deletesATemplate() {
        var template = new AddTemplateUseCase(repository, new FakeSupportedBrokers(), CLOCK)
                .execute(new AddTemplateCommand(collection, aKafkaTemplate("Order created")));

        useCase.execute(new DeleteTemplateCommand(collection, new TemplateId(template.id())));

        assertThat(repository.findById(collection).orElseThrow().templates()).isEmpty();
    }

    @Test
    void failsForAnUnknownTemplate() {
        assertThatThrownBy(() -> useCase.execute(new DeleteTemplateCommand(collection, TestIds.templateId())))
                .isInstanceOf(TemplateNotFoundException.class);
    }
}
