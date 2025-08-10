package com.coupon.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3FileInfo {
    private String fileId;
    private String s3Key;
    private String originalFileName;
    private long fileSize;
    private String bucketName;
}
