package com.oriolvc.topicman.collection.application.usecase;

import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.application.model.CreateCollectionCommand;
import com.oriolvc.topicman.collection.application.model.RenameCollectionCommand;
import com.oriolvc.topicman.collection.domain.exception.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.CollectionName;
import com.oriolvc.topicman.collection.domain.model.TestIds;
import org.junit.jupiter.api.Test;

class RenameCollectionUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final CreateCollectionUseCase create = new CreateCollectionUseCase(repository, CLOCK);
    private final RenameCollectionUseCase useCase = new RenameCollectionUseCase(repository, CLOCK);

    @Test
    void renamesACollection() {
        var created = create.execute(new CreateCollectionCommand(new CollectionName("Orders")));

        var result = useCase.execute(
                new RenameCollectionCommand(new CollectionId(created.id()), new CollectionName("Shipping")));

        assertThat(result.name()).isEqualTo("Shipping");
        assertThat(repository
                        .findById(new CollectionId(created.id()))
                        .orElseThrow()
                        .name())
                .isEqualTo(new CollectionName("Shipping"));
    }

    @Test
    void keepsItsOwnName() {
        var created = create.execute(new CreateCollectionCommand(new CollectionName("Orders")));

        var result = useCase.execute(
                new RenameCollectionCommand(new CollectionId(created.id()), new CollectionName("Orders")));

        assertThat(result.name()).isEqualTo("Orders");
    }

    @Test
    void rejectsAnotherCollectionsName() {
        var orders = create.execute(new CreateCollectionCommand(new CollectionName("Orders")));
        create.execute(new CreateCollectionCommand(new CollectionName("Shipping")));

        assertThatThrownBy(() -> useCase.execute(
                        new RenameCollectionCommand(new CollectionId(orders.id()), new CollectionName("Shipping"))))
                .isInstanceOf(CollectionNameTakenException.class);
    }

    @Test
    void failsForAnUnknownCollection() {
        assertThatThrownBy(() -> useCase.execute(
                        new RenameCollectionCommand(TestIds.collectionId(), new CollectionName("Orders"))))
                .isInstanceOf(CollectionNotFoundException.class);
    }
}
