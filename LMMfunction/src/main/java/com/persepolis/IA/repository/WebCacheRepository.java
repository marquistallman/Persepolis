package com.persepolis.IA.repository;

import com.persepolis.IA.model.WebCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebCacheRepository extends JpaRepository<WebCache, Long> {
    Optional<WebCache> findByCacheKey(String cacheKey);
}