package com.coupon.assignment.domain;

import com.coupon.assignment.entity.FileMeta;
import lombok.Builder;
import lombok.Getter;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;

@Getter
@Builder
public class FileDomain {
    private String fileId;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private LocalDateTime uploadTime;
    private LocalDateTime downloadTime;
    private Boolean status;
    private Resource resource;

    public static FileDomain fromEntity(FileMeta fileMeta) {
        return FileDomain.builder()
                .fileId(fileMeta.getFileId())
                .originalFileName(fileMeta.getOriginalFileName())
                .storedFileName(fileMeta.getStoredFileName())
                .contentType(fileMeta.getContentType())
                .uploadTime(fileMeta.getUploadTime())
                .downloadTime(fileMeta.getDownloadTime())
                .status(fileMeta.getStatus())
                .build();
    }

    public static FileDomain fromResource(Resource resource, FileMeta fileMeta) {
        return FileDomain.builder()
                .fileId(fileMeta.getFileId())
                .originalFileName(fileMeta.getOriginalFileName())
                .storedFileName(fileMeta.getStoredFileName())
                .contentType(fileMeta.getContentType())
                .uploadTime(fileMeta.getUploadTime())
                .downloadTime(fileMeta.getDownloadTime())
                .status(fileMeta.getStatus())
                .resource(resource)
                .build();

    }
}
