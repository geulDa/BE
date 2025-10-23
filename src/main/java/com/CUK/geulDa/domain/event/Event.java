package com.CUK.geulDa.domain.event;

import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDate;

@Entity
@Getter
public class Event extends BaseEntity {

    @Id
    private String id;

    private String title;

    private String body;

    private String address;

    private LocalDate startDate;

    private LocalDate endDate;

    private String externalUrl;

    private String imageUrl;

    // 기본 생성자
    protected Event() {}

    // setter
    public Event(String id, String title, String body, String address,
                 LocalDate startDate, LocalDate endDate,
                 String externalUrl, String imageUrl) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.externalUrl = externalUrl;
        this.imageUrl = imageUrl;
    }
}