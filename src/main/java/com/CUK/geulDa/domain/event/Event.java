package com.CUK.geulDa.domain.event;

import com.CUK.geulDa.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDate;

@Entity
@Getter
public class Event {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String title;

    private String body;

    private String address;

    private LocalDate startDate;

    private LocalDate endDate;

    private String externalUrl;

    private String imageUrl;

    private Boolean isBookmarked;

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
        this.isBookmarked = false;
    }

    // 메서드 관리
    public void toggleBookmark() {
        this.isBookmarked = !this.isBookmarked;
    }

}