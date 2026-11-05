package com.oriolvc.topicman.collection.application.usecase;

import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.application.model.CreateCollectionCommand;
import com.oriolvc.topicman.collection.application.model.DeleteCollectionCommand;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.CollectionName;
import com.oriolvc.topicman.collection.domain.model.TestIds;
import org.junit.jupiter.api.Test;

class DeleteCollectionUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final DeleteCollectionUseCase useCase = new DeleteCollectionUseCase(repository);

    @Test
    void deletesACollection() {
        var created = new CreateCollectionUseCase(repository, CLOCK)
                .execute(new CreateCollectionCommand(new CollectionName("Orders")));

        useCase.execute(new DeleteCollectionCommand(new CollectionId(created.id())));

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void failsForAnUnknownCollection() {
        assertThatThrownBy(() -> useCase.execute(new DeleteCollectionCommand(TestIds.collectionId())))
                .isInstanceOf(CollectionNotFoundException.class);
    }
}
