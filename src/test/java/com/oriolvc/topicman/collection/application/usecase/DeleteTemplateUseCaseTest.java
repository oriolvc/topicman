package com.oriolvc.topicman.collection.application.usecase;

import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.application.model.AddTemplateCommand;
import com.oriolvc.topicman.collection.application.model.CreateCollectionCommand;
import com.oriolvc.topicman.collection.application.model.DeleteTemplateCommand;
import com.oriolvc.topicman.collection.domain.exception.TemplateNotFoundException;
import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.CollectionName;
import com.oriolvc.topicman.collection.domain.model.TemplateId;
import com.oriolvc.topicman.collection.domain.model.TestIds;
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
