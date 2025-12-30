package com.persepolis.IA.repository;

import com.persepolis.IA.model.WallpaperItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WallpaperRepository extends JpaRepository<WallpaperItem, Long> {
    List<WallpaperItem> findByHtmlContentContainingIgnoreCase(String keyword);
    Optional<WallpaperItem> findByUrl(String url);
}

