package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Course;
import com.example.studentmanagement.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByCourseOrderBySemesterAscNameAsc(Course course);

    List<Section> findByCourseIdAndSemester(Long courseId, int semester);

    List<Section> findBySemester(int semester);

    Optional<Section> findByCourseAndSemesterAndName(Course course, int semester, String name);
}
