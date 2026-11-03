package com.oriolvc.topicman.collection.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "collection")
class CollectionEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "collection_id", nullable = false)
    @OrderColumn(name = "sort_order")
    private List<TemplateEntity> templates = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Null until the first insert, which is how Spring Data tells a new entity from an existing one.
    // Every template change touches updatedAt, so it also guards the templates against lost updates.
    @Version
    private @Nullable Long version;

    protected CollectionEntity() {}

    CollectionEntity(long id, String name, List<TemplateEntity> templates, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.templates = new ArrayList<>(templates);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Updates this managed row. Templates already stored are updated in place rather than deleted
     * and inserted again, so a change never trips the unique template name constraint midway.
     */
    void update(String name, List<TemplateEntity> wanted, Instant updatedAt) {
        this.name = name;
        var stored = templates.stream().collect(Collectors.toMap(TemplateEntity::id, Function.identity()));
        var next = wanted.stream()
                .map(template -> {
                    var existing = stored.get(template.id());
                    if (existing == null) {
                        return template;
                    }
                    existing.copyFrom(template);
                    return existing;
                })
                .toList();
        templates.clear();
        templates.addAll(next);
        this.updatedAt = updatedAt;
    }

    long id() {
        return id;
    }

    String name() {
        return name;
    }

    List<TemplateEntity> templates() {
        return templates;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
