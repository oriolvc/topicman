package com.oriolvc.topicman.connection.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ConnectionJpaRepository extends JpaRepository<ConnectionEntity, Long> {

    List<ConnectionEntity> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    @Query(value = "SELECT nextval('broker_connection_id_seq')", nativeQuery = true)
    long nextId();
}
