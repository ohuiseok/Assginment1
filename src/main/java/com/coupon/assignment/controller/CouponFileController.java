package com.coupon.assignment.controller;

import com.coupon.assignment.common.dto.CommonResponse;
import com.coupon.assignment.domain.FileDomain;
import com.coupon.assignment.dto.FileUploadResponse;
import com.coupon.assignment.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/coupons/files")
@RequiredArgsConstructor
public class CouponFileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<CommonResponse<?>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        FileDomain fileDomain = fileStorageService.store(file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        CommonResponse.success("file.create.success", FileUploadResponse.fromDomain(fileDomain))
                );
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downLoadFile(@PathVariable String fileId) {
        FileDomain fileDomain = fileStorageService.retrieve(fileId);
        String contentDisposition = "attachment; filename=\"" + fileDomain.getStoredFileName() + "\"";

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(fileDomain.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(fileDomain.getResource());
    }
}
