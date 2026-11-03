package com.oriolvc.topicman.collection.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface EnvironmentJpaRepository extends JpaRepository<EnvironmentEntity, Long> {

    List<EnvironmentEntity> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    @Query(value = "SELECT nextval('environment_id_seq')", nativeQuery = true)
    long nextId();
}
