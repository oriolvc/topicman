package com.oriolvc.topicman.collection.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CollectionJpaRepository extends JpaRepository<CollectionEntity, UUID> {

    List<CollectionEntity> findAllByOrderByNameAsc();

    boolean existsByName(String name);
}
