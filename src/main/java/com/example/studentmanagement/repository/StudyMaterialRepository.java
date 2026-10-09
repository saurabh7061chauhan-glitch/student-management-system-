package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Course;
import com.example.studentmanagement.entity.Section;
import com.example.studentmanagement.entity.StudyMaterial;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    List<StudyMaterial> findByTeacherOrderByUploadDateDesc(Teacher teacher);

    List<StudyMaterial> findBySubjectOrderByUploadDateDesc(Subject subject);

    List<StudyMaterial> findBySubjectInOrderByUploadDateDesc(List<Subject> subjects);

    List<StudyMaterial> findByCourseAndSemesterOrderByUploadDateDesc(Course course, int semester);

    List<StudyMaterial> findBySectionOrderByUploadDateDesc(Section section);
}
