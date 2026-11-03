package com.oriolvc.topicman.collection.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A named, ordered list of message templates. Template names are unique within a collection;
 * collection names are unique among collections, which the repository enforces.
 */
public final class Collection {

    private final CollectionId id;
    private final Instant createdAt;
    private final List<MessageTemplate> templates;
    private CollectionName name;
    private Instant updatedAt;

    private Collection(
            CollectionId id,
            CollectionName name,
            List<MessageTemplate> templates,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.templates = new ArrayList<>();
        Objects.requireNonNull(templates, "templates").forEach(this::requireNameFree);
        this.templates.addAll(templates);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** @param id a new id, from {@link CollectionRepository#nextId()} */
    public static Collection create(CollectionId id, CollectionName name, Instant now) {
        return new Collection(id, name, List.of(), now, now);
    }

    /**
     * A new collection with the templates of an imported file, in order. Two templates with the
     * same name make the file invalid, rather than one taking the other's name.
     *
     * @param id a new id, from {@link CollectionRepository#nextId()}
     * @throws InvalidTemplateException if two templates have the same name
     */
    public static Collection fromFile(
            CollectionId id, CollectionName name, List<MessageTemplate> templates, Instant now) {
        var collection = create(id, name, now);
        for (var template : templates) {
            try {
                collection.addTemplate(template, now);
            } catch (TemplateNameTakenException e) {
                throw new InvalidTemplateException("The file has more than one template named '"
                        + template.name().value() + "'");
            }
        }
        return collection;
    }

    public static Collection rehydrate(
            CollectionId id,
            CollectionName name,
            List<MessageTemplate> templates,
            Instant createdAt,
            Instant updatedAt) {
        return new Collection(id, name, templates, createdAt, updatedAt);
    }

    public void rename(CollectionName newName, Instant now) {
        name = Objects.requireNonNull(newName, "newName");
        touch(now);
    }

    /** Appends a template after the existing ones. */
    public void addTemplate(MessageTemplate template, Instant now) {
        requireNameFree(template);
        templates.add(template);
        touch(now);
    }

    /** Replaces the template with the same id, keeping its place in the list. */
    public void replaceTemplate(MessageTemplate template, Instant now) {
        var index = indexOf(template.id());
        requireNameFree(template);
        templates.set(index, template);
        touch(now);
    }

    public void removeTemplate(TemplateId templateId, Instant now) {
        templates.remove(indexOf(templateId));
        touch(now);
    }

    public Optional<MessageTemplate> template(TemplateId templateId) {
        return templates.stream()
                .filter(template -> template.id().equals(templateId))
                .findFirst();
    }

    private int indexOf(TemplateId templateId) {
        for (int index = 0; index < templates.size(); index++) {
            if (templates.get(index).id().equals(templateId)) {
                return index;
            }
        }
        throw new TemplateNotFoundException(templateId);
    }

    /** Rejects a template whose name another template, with a different id, already has. */
    private void requireNameFree(MessageTemplate candidate) {
        var taken = templates.stream()
                .anyMatch(template -> template.name().equals(candidate.name())
                        && !template.id().equals(candidate.id()));
        if (taken) {
            throw new TemplateNameTakenException(candidate.name());
        }
    }

    private void touch(Instant now) {
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public CollectionId id() {
        return id;
    }

    public CollectionName name() {
        return name;
    }

    public List<MessageTemplate> templates() {
        return Collections.unmodifiableList(templates);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof Collection collection && id.equals(collection.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
