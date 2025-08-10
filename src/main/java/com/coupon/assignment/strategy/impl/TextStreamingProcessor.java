package com.coupon.assignment.strategy.impl;

import com.coupon.assignment.strategy.FileStreamingProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TextStreamingProcessor implements FileStreamingProcessor {

    @Override
    public List<String> extractUserIds(InputStream inputStream) throws Exception {

        List<String> userIds = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;

                // 텍스트 라인 처리
                String userId = processTextLine(line, lineCount);
                if (userId != null && !userId.trim().isEmpty()) {
                    userIds.add(userId.trim());
                }

                // 진행률 로깅 (1000라인마다)
                if (lineCount % 1000 == 0) {
                    log.debug("Text 진행률: {} lines, {} size", lineCount, userIds.size());
                }

                log.info("Text completed - Total : {} lines, {} size", lineCount, userIds.size());
            }

        }

        return userIds;
    }

    /**
     * 텍스트 라인에서 사용자 ID 추출
     */
    private String processTextLine(String line, int lineNumber) {
        try {
            String trimmedLine = line.trim();

            // 빈 라인 스킵
            if (trimmedLine.isEmpty()) {
                return null;
            }

            // 주석 라인 스킵 (# 또는 // 로 시작)
            if (trimmedLine.startsWith("#") || trimmedLine.startsWith("//")) {
                return null;
            }

            // 구분자가 있는 경우 첫 번째 토큰만 사용 (탭, 공백, 콤마 등)
            String[] tokens = trimmedLine.split("[\\s,\\t]+");
            String userId = tokens[0].trim();

            // 유효성 검사
            if (userId.isEmpty() || "null".equalsIgnoreCase(userId)) {
                return null;
            }

            return userId;

        } catch (Exception e) {
            log.warn(" Error  Error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getSupportedExtension() {
        return "txt";
    }

    @Override
    public int getBufferSize() {
        return 8192; // 8KB for text files
    }

    @Override
    public int getHeaderLinesToSkip() {
        return 0; // 텍스트 파일은 일반적으로 헤더가 없음
    }

    @Override
    public int getBatchSize() {
        return 2000; // 텍스트는 더 간단하므로 더 많이 배치 처리
    }
}
