package com.oriolvc.topicman.collection.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EnvironmentJpaRepository extends JpaRepository<EnvironmentEntity, UUID> {

    List<EnvironmentEntity> findAllByOrderByNameAsc();

    boolean existsByName(String name);
}
