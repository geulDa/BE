package com.CUK.geulDa.domain.member.repository;

import com.CUK.geulDa.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, String> {

}
