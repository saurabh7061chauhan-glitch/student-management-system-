package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.QrAttendanceSession;
import com.example.studentmanagement.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrAttendanceSessionRepository extends JpaRepository<QrAttendanceSession, Long> {

    Optional<QrAttendanceSession> findBySessionToken(String sessionToken);

    List<QrAttendanceSession> findByTeacherAndActiveTrue(Teacher teacher);

    List<QrAttendanceSession> findByActiveTrue();
}
