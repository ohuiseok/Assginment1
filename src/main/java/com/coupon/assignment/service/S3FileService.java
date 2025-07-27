package com.coupon.assignment.service;

import com.coupon.assignment.dto.S3FileInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileService {

    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${aws.s3.folder.uploads:uploads}")
    private String uploadsFolder;
    
    @Value("${aws.s3.folder.processed:processed}")
    private String processedFolder;
    
    @Value("${aws.s3.folder.failed:failed}")
    private String failedFolder;
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3FileInfo uploadFileStreaming(MultipartFile file, BufferedInputStream bufferedInputStream) throws IOException {
        String fileId = UUID.randomUUID().toString();
        String originalFileName = file.getOriginalFilename();
        String s3Key = generateS3Key(uploadsFolder, fileId, originalFileName);
        
        try {
            assert originalFileName != null;
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(getContentType(originalFileName))
                .contentLength(file.getSize())
                .metadata(Map.of(
                    "original-filename", originalFileName,
                    "file-id", fileId,
                    "upload-timestamp", String.valueOf(System.currentTimeMillis())
                ))
                .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(bufferedInputStream, file.getSize()));
            
            return new S3FileInfo(fileId, s3Key, originalFileName, file.getSize(), bucketName);
            
        } catch (Exception e) {
            throw new IOException("S3 업로드 실패: " + e.getMessage(), e);
        }
    }

    public InputStream getFileInputStreamForStreaming(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
            
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            
            return s3Object;
        } catch (Exception e) {
            throw new RuntimeException("S3 다운로드 실패: " + e.getMessage(), e);
        }
    }

    public String moveToProcessedFolder(String originalS3Key, String fileId) {
        String fileName = extractFileNameFromS3Key(originalS3Key);
        String processedS3Key = generateS3Key(processedFolder, fileId, fileName);

        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(originalS3Key)
                .destinationBucket(bucketName)
                .destinationKey(processedS3Key)
                .build();

            s3Client.copyObject(copyRequest);
            deleteFile(originalS3Key);

            return processedS3Key;

        } catch (Exception e) {
            throw new RuntimeException("파일 이동 실패: " + e.getMessage(), e);
        }
    }

    public String moveToFailedFolder(String originalS3Key, String fileId) {
        String fileName = extractFileNameFromS3Key(originalS3Key);
        String failedS3Key = generateS3Key(failedFolder, fileId, fileName);

        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(originalS3Key)
                .destinationBucket(bucketName)
                .destinationKey(failedS3Key)
                .build();

            s3Client.copyObject(copyRequest);
            return failedS3Key;

        } catch (Exception e) {
            throw new RuntimeException("실패 파일 이동 실패: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
            
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception ignored) {
        }
    }
    
    private String generateS3Key(String folder, String fileId, String originalFileName) {
        String fileName = extractFileName(originalFileName);
        return String.format("%s/%s_%s", folder, fileId, fileName);
    }
    
    private String extractFileName(String fileName) {
        if (fileName == null) return "unknown_file";
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    private String extractFileNameFromS3Key(String s3Key) {
        return s3Key.substring(s3Key.lastIndexOf('/') + 1);
    }
    
    private String getContentType(String fileName) {
        String extension = getFileExtension(fileName);
        return switch (extension) {
            case "csv" -> "text/csv";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            default -> "application/octet-stream";
        };
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}
