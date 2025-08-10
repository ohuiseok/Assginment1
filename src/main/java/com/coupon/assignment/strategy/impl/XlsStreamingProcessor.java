package com.coupon.assignment.strategy.impl;

import com.coupon.assignment.strategy.FileStreamingProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class XlsStreamingProcessor implements FileStreamingProcessor {

    @Override
    public List<String> extractUserIds(InputStream inputStream) throws Exception {
        List<String> userIds = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            boolean isFirstRow = true;
            int rowCount = 0;

            for (Row row : sheet) {
                rowCount++;

                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }

                String userId = processExcelRow(row, rowCount);
                if (userId != null && !userId.trim().isEmpty()) {
                    userIds.add(userId.trim());
                }

                if (rowCount % 1000 == 0) {
                    log.debug("XLS 진행률: {} lines, {} size", rowCount, userIds.size());
                }
            }

            log.info("XLS completed - Total : {} lines, {} size", rowCount, userIds.size());
        }

        return userIds;
    }

    private String processExcelRow(Row row, int rowNumber) {
        try {
            // 첫 번째 셀에서 사용자 ID 추출
            Cell firstCell = row.getCell(0);
            if (firstCell == null) {
                return null;
            }

            String userId = getCellValueAsString(firstCell);

            // 유효성 검사
            if (userId == null || userId.trim().isEmpty() || "null".equalsIgnoreCase(userId)) {
                return null;
            }

            return userId.trim();

        } catch (Exception e) {
            log.warn("XLS 행 처리 에러: {}", e.getMessage());
            return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // 숫자를 정수로 변환 (소수점 제거)
                        double numericValue = cell.getNumericCellValue();
                        if (numericValue == Math.floor(numericValue)) {
                            return String.valueOf((long) numericValue);
                        } else {
                            return String.valueOf(numericValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                case BLANK:
                    return "";
                default:
                    return cell.toString();
            }
        } catch (Exception e) {
            log.warn("셀 값 변환 에러: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getSupportedExtension() {
        return "xls";
    }

    @Override
    public int getBufferSize() {
        return 16384;
    }

    @Override
    public int getHeaderLinesToSkip() {
        return 1;
    }

    @Override
    public int getBatchSize() {
        return 1000;
    }

    @Override
    public long getMaxFileSize() {
        return 100 * 1024 * 1024;
    }
}
