package com.CUK.geulDa.domain.member;

import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.stamp.Stamp;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Member extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    private String profileImageUrl;

    // JPA 전용
    // 외부에서 Member 객체 생성 방지
    protected Member() {}


    // Setter
    public Member(String id, String email, String name, String profileImageUrl) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    // 연관관계
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stamp> stamps = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPostCard> postcards = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> recommendations = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberEventBookmark> bookmarkedEvents = new ArrayList<>();
}
