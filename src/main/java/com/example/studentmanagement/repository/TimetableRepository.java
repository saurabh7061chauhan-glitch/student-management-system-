package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Course;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.entity.Teacher;
import com.example.studentmanagement.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findByCourseAndSemesterOrderByDayOfWeekAscStartTimeAsc(Course course, int semester);

    List<TimetableEntry> findByCourseIdAndSemester(Long courseId, int semester);

    List<TimetableEntry> findByCourseAndSemester(Course course, int semester);

    List<TimetableEntry> findBySubject(Subject subject);

    List<TimetableEntry> findByTeacher(Teacher teacher);

    List<TimetableEntry> findByCourseAndSemesterAndDayOfWeek(Course course, int semester, String dayOfWeek);
}
