package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Notice;
import com.example.studentmanagement.entity.NoticeReadStatus;
import com.example.studentmanagement.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeReadStatusRepository extends JpaRepository<NoticeReadStatus, Long> {

    Optional<NoticeReadStatus> findByNoticeAndStudent(Notice notice, Student student);

    boolean existsByNoticeAndStudent(Notice notice, Student student);

    List<NoticeReadStatus> findByStudent(Student student);
}
