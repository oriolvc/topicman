package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.CollectionNotFoundException;
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
        assertThatThrownBy(() -> useCase.execute(new DeleteCollectionCommand(CollectionId.newId())))
                .isInstanceOf(CollectionNotFoundException.class);
    }
}
