package com.coupon.assignment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CouponProcessingMessage {
    @JsonProperty("fileId")
    private String fileId;
    
    @JsonProperty("s3Key")
    private String s3Key;
    
    @JsonProperty("fileName")
    private String fileName;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("retryCount")
    private int retryCount = 0;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    public CouponProcessingMessage(String fileId, String s3Key, String fileName, String userId) {
        this.fileId = fileId;
        this.s3Key = s3Key;
        this.fileName = fileName;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }
    
    public void incrementRetryCount() { 
        this.retryCount++; 
    }
}
