package com.coupon.assignment.service;

import com.coupon.assignment.dto.CouponProcessingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 쿠폰 처리 메시지 발행
     */
    public void publishCouponProcessingMessage(CouponProcessingMessage message) {

        kafkaTemplate.send("coupon-processing", message.getFileId(), message)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("error fileId={}", message.getFileId(), throwable);
                    }
                });
    }

}
