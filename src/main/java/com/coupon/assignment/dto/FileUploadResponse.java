package com.coupon.assignment.dto;


import com.coupon.assignment.domain.FileDomain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FileUploadResponse {
    private String fileId;

    public static FileUploadResponse fromDomain(FileDomain fileDomain) {
        return new FileUploadResponse(fileDomain.getFileId());
    }
}