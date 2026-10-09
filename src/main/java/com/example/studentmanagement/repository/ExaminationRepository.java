package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findAllByOrderByStartDateDesc();
    List<Examination> findByStatus(String status);
    List<Examination> findByAcademicYearAndSemester(String academicYear, int semester);
}
