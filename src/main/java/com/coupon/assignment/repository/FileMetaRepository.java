package com.coupon.assignment.repository;

import com.coupon.assignment.entity.FileMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileMetaRepository extends JpaRepository<FileMeta, String> {
    Optional<FileMeta> findByFileId(String fileId);
}