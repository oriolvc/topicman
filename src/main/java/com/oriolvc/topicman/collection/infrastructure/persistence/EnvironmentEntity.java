package com.oriolvc.topicman.collection.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "environment")
class EnvironmentEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(name = "environment_variable", joinColumns = @JoinColumn(name = "environment_id"))
    @OrderColumn(name = "sort_order")
    private List<NameValueEmbeddable> variables = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Null until the first insert, which is how Spring Data tells a new entity from an existing one.
    @Version
    private @Nullable Long version;

    protected EnvironmentEntity() {}

    EnvironmentEntity(long id, String name, List<NameValueEmbeddable> variables, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.variables = new ArrayList<>(variables);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void update(String name, List<NameValueEmbeddable> variables, Instant updatedAt) {
        this.name = name;
        this.variables.clear();
        this.variables.addAll(variables);
        this.updatedAt = updatedAt;
    }

    long id() {
        return id;
    }

    String name() {
        return name;
    }

    List<NameValueEmbeddable> variables() {
        return variables;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
