package com.coupon.assignment.service;

import com.coupon.assignment.dto.CouponProcessingMessage;
import com.coupon.assignment.entity.FileMeta;
import com.coupon.assignment.repository.FileMetaRepository;
import com.coupon.assignment.strategy.FileStreamingProcessor;
import com.coupon.assignment.strategy.FileStreamingProcessorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaConsumerService {

    private final S3FileService s3FileService;

    private final FileMetaRepository fileMetaRepository;

    private final CouponService couponService;

    private final FileStreamingProcessorFactory fileStreamingProcessorFactory;


    /**
     * 쿠폰 처리 메시지 처리 - 전략 패턴으로 파일 읽어서 쿠폰 발급
     */
    @KafkaListener(topics = "coupon-processing", groupId = "coupon-processing-group", concurrency = "3")
    public void handleCouponProcessing(@Payload CouponProcessingMessage message,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset,
                                       Acknowledgment acknowledgment) {
        try {

            // 전략 패턴으로 스트리밍 파일 처리 및 쿠폰 발급
            processCouponForFileStreaming(message);

            // 처리 완료 - FileMeta 상태 업데이트
            updateFileMetaStatus(message.getFileId(), true);

            // S3에서 processed 폴더로 이동
            s3FileService.moveToProcessedFolder(message.getS3Key(), message.getFileId());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("error: {}", message.getFileId(), e);

            try {
                // 실패 처리
                updateFileMetaStatus(message.getFileId(), false);
                s3FileService.moveToFailedFolder(message.getS3Key(), message.getFileId());
            } catch (Exception failureHandlingException) {
                log.error("error: {}", message.getFileId(), failureHandlingException);
            }

            acknowledgment.acknowledge(); // 무한 재시도 방지
        }
    }

    /**
     * 스트리밍 방식으로 파일 처리 및 쿠폰 발급 - 전략 패턴 적용
     */
    private void processCouponForFileStreaming(CouponProcessingMessage message) throws Exception {

        String fileExtension = getFileExtension(message.getFileName());

        // 팩토리에서 적절한 전략 선택
        Optional<FileStreamingProcessor> processorOpt = fileStreamingProcessorFactory.getProcessor(fileExtension);

        if (processorOpt.isEmpty()) {
            throw new IllegalArgumentException("지원되지 않는 파일 형식: " + fileExtension +
                    ". 지원 형식: " + fileStreamingProcessorFactory.getSupportedExtensions());
        }

        FileStreamingProcessor processor = processorOpt.get();

        List<String> userIds;

        // S3에서 스트리밍으로 파일 읽기 및 전략 패턴으로 처리
        try (InputStream inputStream = s3FileService.getFileInputStreamForStreaming(message.getS3Key())) {
            userIds = processor.extractUserIds(inputStream);
        }

        // 추출된 사용자들에게 쿠폰 발급
        if (!ObjectUtils.isEmpty(userIds)) {
            couponService.issueCouponsToUsers(userIds);
        } else {
            log.warn("empty : {} ", message.getFileId());
        }
    }

    /**
     * FileMeta 상태 업데이트
     */
    private void updateFileMetaStatus(String fileId, boolean status) {
        try {
            FileMeta fileMeta = fileMetaRepository.findById(fileId).orElseThrow(() -> new Exception(""));
            fileMeta.setStatus(status);
            if (status) {
                fileMeta.setDownloadTime(LocalDateTime.now());
            }
            fileMetaRepository.save(fileMeta);
        } catch (Exception e) {
            log.error("error: {}", fileId, e);
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }

        return "";
    }
}
