package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByPostedDateDesc();

    List<Notice> findByTargetAudienceInOrderByPostedDateDesc(List<String> audiences);

    List<Notice> findTop5ByOrderByPostedDateDesc();
}
