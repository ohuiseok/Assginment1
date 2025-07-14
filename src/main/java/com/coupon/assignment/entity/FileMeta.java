package com.coupon.assignment.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_meta")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileMeta {
    @Id
    private String fileId;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private LocalDateTime uploadTime;
    private LocalDateTime downloadTime;
    private Boolean status; // 스프링 배치에 의해 주기적으로 읽어서 쿠폰을 전달할 때 쓰일 플래그 값

}
