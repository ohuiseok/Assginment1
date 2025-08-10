package com.coupon.assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CouponService {
    /**
     * 여러 사용자에게 쿠폰 발급
     */
    public void issueCouponsToUsers(List<String> userIds) {

        int successCount = 0;

        for (String userId : userIds) {
            issueCoupon(userId);
            successCount++;
        }

        log.info("Coupon completed Total: {}", successCount);
    }

    /**
     * 개별 사용자에게 쿠폰 발급
     */
    public void issueCoupon(String userId) {
        try {
            String couponCode = generateCouponCode();
            log.info("쿠폰이 발급되었습니다. user {}, coupon {}", userId, couponCode);

        } catch (Exception e) {
            log.error("error {}", e);
            throw new RuntimeException("Coupon issuance failed for user: " + userId, e);
        }
    }

    /**
     * 쿠폰 코드 생성
     */
    private String generateCouponCode() {
        return "COUPON_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyymmdd")) + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

}
