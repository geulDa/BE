package com.CUK.geulDa.domain.member;

import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.member.constant.Provider;
import com.CUK.geulDa.domain.member.constant.Role;
import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.stamp.Stamp;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "member",
    indexes = @Index(name = "idx_provider_provider_id", columnList = "provider, providerId")
)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(nullable = false, length = 100)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // 연관관계
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stamp> stamps = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPostCard> postcards = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> recommendations = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberEventBookmark> bookmarkedEvents = new ArrayList<>();

    @Builder
    public Member(String email, String name, String profileImageUrl,
                  Provider provider, String providerId, Role role) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    // 프로필 업데이트
    public void updateProfile(String name, String profileImageUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}
