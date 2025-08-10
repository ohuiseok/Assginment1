package com.coupon.assignment.repository;

import com.coupon.assignment.entity.FileMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileMetaRepository extends JpaRepository<FileMeta, String> {

    Page<FileMeta> findByStatusFalse(Pageable pageable);

    List<FileMeta> findByStatusFalse();

    List<FileMeta> findByStatusTrue();

    List<FileMeta> findByUploadTimeAfter(LocalDateTime uploadTime);

    List<FileMeta> findByUploadTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT f FROM FileMeta f WHERE f.originalFileName LIKE %:extension")
    List<FileMeta> findByFileExtension(String extension);

    @Query("SELECT f FROM FileMeta f WHERE f.status = false AND f.uploadTime < :cutoffTime")
    List<FileMeta> findUnprocessedOldFiles(LocalDateTime cutoffTime);
}
