package com.productcatalog.infrastructure.persistence.ports;

import com.productcatalog.infrastructure.persistence.entities.TrackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackJpaRepository extends JpaRepository<TrackEntity, String>, JpaSpecificationExecutor<TrackEntity> {
    List<TrackEntity> findByProductId(String productId);
    void deleteByProductId(String string);
}
