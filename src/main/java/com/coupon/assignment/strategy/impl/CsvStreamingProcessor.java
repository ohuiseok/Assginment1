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
public class CsvStreamingProcessor implements FileStreamingProcessor {

    @Override
    public List<String> extractUserIds(InputStream inputStream) throws Exception {

        List<String> userIds = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstLine = true;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;

                // 헤더 라인 스킵
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // CSV 라인 처리
                String userId = processCsvLine(line, lineCount);
                if (userId != null && !userId.trim().isEmpty()) {
                    userIds.add(userId.trim());
                }

                // 진행률 로깅 (1000라인마다)
                if (lineCount % 1000 == 0) {
                    log.debug("CSV 진행률: {} lines, {} size", lineCount, userIds.size());
                }

            }

            log.info("CSV completed - Total : {} lines, {} size", lineCount, userIds.size());
        }

        return userIds;
    }

    /**
     * CSV 라인에서 사용자 ID 추출
     */
    private String processCsvLine(String line, int lineNumber) {
        try {
            // CSV 파싱 (간단한 구현 - 실제로는 Apache Commons CSV 사용 권장)
            String[] columns = line.split(",");

            if (columns.length > 0) {
                String userId = columns[0].trim();

                // 따옴표 제거
                if (userId.startsWith("\"") && userId.endsWith("\"")) {
                    userId = userId.substring(1, userId.length() - 1);
                }

                // 빈 값이나 유효하지 않은 값 체크
                if (userId.isEmpty() || "null".equalsIgnoreCase(userId)) {
                    return null;
                }

                return userId;
            } else {
                return null;
            }

        } catch (Exception e) {
            log.warn(" Error  Error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getSupportedExtension() {
        return "csv";
    }

    @Override
    public int getBufferSize() {
        return 16384; // 16KB for CSV files
    }

    @Override
    public int getHeaderLinesToSkip() {
        return 1; // CSV는 일반적으로 첫 번째 라인이 헤더
    }

    @Override
    public int getBatchSize() {
        return 1000; // 1000개씩 배치 처리
    }
}
