package com.coupon.assignment.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor // Component 등록으로 스프링빈 생성 및 관리
@AllArgsConstructor
@Component //  staticMessageSource에 MessageSource 빈을 주입받기 위해 필요
public class CommonResponse<T> {
    private String message;
    private T data;
    private LocalDateTime timestamp;

    private static MessageSource staticMessageSource;

    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        CommonResponse.staticMessageSource = messageSource;
    }

    public static <T> CommonResponse<T> success(String messageKey, T data) {
        String resolvedMessage;
        try {
            resolvedMessage = staticMessageSource.getMessage(
                    messageKey,
                    null,
                    LocaleContextHolder.getLocale()
            );
        } catch (Exception e) {
            resolvedMessage = "Operation completed successfully";
        }
        return new CommonResponse<>(resolvedMessage, data, LocalDateTime.now());
    }

    public static CommonResponse<Void> success(String messageKey) {
        String resolvedMessage;
        try {
            resolvedMessage = staticMessageSource.getMessage(
                    messageKey,
                    null,
                    LocaleContextHolder.getLocale()
            );
        } catch (Exception e) {
            resolvedMessage = "Operation completed successfully";
        }
        return new CommonResponse<>(resolvedMessage, null, LocalDateTime.now());
    }


}
