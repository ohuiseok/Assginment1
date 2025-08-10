package com.coupon.assignment.common.config;

import com.coupon.assignment.dto.CouponProcessingMessage;
import com.coupon.assignment.entity.FileMeta;
import com.coupon.assignment.repository.FileMetaRepository;
import com.coupon.assignment.service.KafkaProducerService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class BatchConfig {

    @Autowired
    private FileMetaRepository fileMetaRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Bean
    public Job retryFailedCouponJob(JobRepository jobRepository, Step retryFailedCouponStep) {
        return new JobBuilder("retryFailedCouponJob", jobRepository)
                .start(retryFailedCouponStep)
                .build();
    }

    @Bean
    public Step retryFailedCouponStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("retryFailedCouponStep", jobRepository)
                .<FileMeta, CouponProcessingMessage>chunk(100, transactionManager)
                .reader(failedFileMetaReader())
                .processor(fileMetaProcessor())
                .writer(couponMessageWriter())
                .build();
    }

    @Bean
    public RepositoryItemReader<FileMeta> failedFileMetaReader() {
        return new RepositoryItemReaderBuilder<FileMeta>()
                .name("failedFileMetaReader")
                .repository(fileMetaRepository)
                .methodName("findByStatusFalse") // status가 false인 실패한 파일들
                .sorts(Map.of("uploadTime", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<FileMeta, CouponProcessingMessage> fileMetaProcessor() {
        return fileMeta -> {
            // FileMeta를 CouponProcessingMessage로 변환
            return new CouponProcessingMessage(
                    fileMeta.getFileId(),
                    fileMeta.getStoredFileName(), // S3 키로 사용
                    fileMeta.getOriginalFileName(),
                    "system" // 재시도는 시스템에서 수행
            );
        };
    }

    @Bean
    public ItemWriter<CouponProcessingMessage> couponMessageWriter() {
        return messages -> {
            for (CouponProcessingMessage message : messages) {
                // Kafka로 재시도 메시지 전송
                kafkaProducerService.publishCouponProcessingMessage(message);
            }
        };
    }
}
