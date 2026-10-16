package com.oriolvc.topicman.connection.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ConnectionJpaRepository extends JpaRepository<ConnectionEntity, UUID> {

    List<ConnectionEntity> findAllByOrderByNameAsc();

    boolean existsByName(String name);
}
