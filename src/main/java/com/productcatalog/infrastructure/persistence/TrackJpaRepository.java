package com.productcatalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackJpaRepository extends JpaRepository<TrackEntity, String> {
    List<TrackEntity> findByProductId(String productId);
}