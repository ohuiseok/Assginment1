package com.coupon.assignment.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class PoiConfig {

    @PostConstruct
    public void configurePoiLimits() {
        try {
            // 기본값: 100MB -> 1GB로 증가 (대용량 파일 처리) 혹시 모를 처리
            IOUtils.setByteArrayMaxOverride(1024 * 1024 * 1024); // 1GB

            
        } catch (Exception e) {
            log.error("POI 설정 중 오류 발생", e);
        }
    }
}
