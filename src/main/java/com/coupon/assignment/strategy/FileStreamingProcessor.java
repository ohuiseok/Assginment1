package com.coupon.assignment.strategy;

import java.io.InputStream;
import java.util.List;

/**
 * 파일 스트리밍 처리 전략 인터페이스
 */
public interface FileStreamingProcessor {

    /**
     * 스트리밍 방식으로 파일에서 사용자 ID 추출
     */
    List<String> extractUserIds(InputStream inputStream) throws Exception;

    /**
     * 지원하는 파일 확장자 반환
     */
    String getSupportedExtension();

    /**
     * 스트리밍 처리를 위한 버퍼 크기 반환
     */
    default int getBufferSize() {
        return 8192;
    }

    /**
     * 처리 가능한 최대 파일 크기 반환 (바이트)
     */
    default long getMaxFileSize() {
        return 100 * 1024 * 1024; // 100MB 기본값
    }

    /**
     * 파일 처리 시 스킵할 헤더 라인 수
     */
    default int getHeaderLinesToSkip() {
        return 1;
    }

    /**
     * 배치 처리 단위 (메모리 효율성을 위해)
     */
    default int getBatchSize() {
        return 1000;
    }
}
