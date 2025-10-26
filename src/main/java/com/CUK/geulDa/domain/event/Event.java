package com.CUK.geulDa.domain.event;

import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String address;

    private LocalDate startDate;

    private LocalDate endDate;

    private String externalUrl;

    private String imageUrl;

    @Builder
    public Event(String title, String body, String address,
                 LocalDate startDate, LocalDate endDate,
                 String externalUrl, String imageUrl) {
        this.title = title;
        this.body = body;
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.externalUrl = externalUrl;
        this.imageUrl = imageUrl;
    }
}