package com.coupon.assignment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledBatchService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledBatchService.class);
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job retryFailedCouponJob;
    
    /**
     * 매시간마다 실패한 쿠폰 처리 재시도
     */
    @Scheduled(cron = "0 0 * * * ?") // 매 시간 정각에 실행
    public void retryFailedCouponProcessing() {
        try {
            logger.info("🔄 Starting scheduled retry for failed coupon processing...");
            
            // Job 파라미터 생성 (실행 시간을 파라미터로 추가하여 고유성 보장)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            
            // 배치 Job 실행
            JobExecution jobExecution = jobLauncher.run(retryFailedCouponJob, jobParameters);
            
            logger.info("✅ Scheduled retry batch completed - Status: {}, Exit Code: {}", 
                       jobExecution.getStatus(), jobExecution.getExitStatus().getExitCode());
            
        } catch (Exception e) {
            logger.error("❌ Failed to execute scheduled retry batch", e);
        }
    }
    
    /**
     * 테스트용 수동 배치 실행 (매 5분마다)
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행 (테스트용)
    public void testRetryFailedCouponProcessing() {
        try {
            logger.info("🧪 Starting test retry for failed coupon processing...");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("test-timestamp", System.currentTimeMillis())
                    .toJobParameters();
            
            JobExecution jobExecution = jobLauncher.run(retryFailedCouponJob, jobParameters);
            
            logger.info("✅ Test retry batch completed - Status: {}", jobExecution.getStatus());
            
        } catch (Exception e) {
            logger.error("❌ Failed to execute test retry batch", e);
        }
    }
}
