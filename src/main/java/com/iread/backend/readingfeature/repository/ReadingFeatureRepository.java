package com.iread.backend.readingfeature.repository;

import com.iread.backend.readingfeature.domain.ReadingFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReadingFeatureRepository extends JpaRepository<ReadingFeatureEntity, Long> {

    Optional<ReadingFeatureEntity> findByFeatureCode(String featureCode);

    List<ReadingFeatureEntity> findAllByFeatureCodeIn(Collection<String> featureCodes);
}
