package com.oriolvc.topicman.collection.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CollectionJpaRepository extends JpaRepository<CollectionEntity, Long> {

    List<CollectionEntity> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    @Query(value = "SELECT nextval('collection_id_seq')", nativeQuery = true)
    long nextId();

    @Query(value = "SELECT nextval('message_template_id_seq')", nativeQuery = true)
    long nextTemplateId();
}
