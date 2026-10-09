package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Department;
import com.example.studentmanagement.entity.Course;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByStudentIdIgnoreCase(String studentId);

    Optional<Student> findByEmail(String email);

    Optional<Student> findByEmailIgnoreCase(String email);

    Optional<Student> findByUser(User user);

    Optional<Student> findByUserUsername(String username);

    boolean existsByStudentId(String studentId);

    boolean existsByEmail(String email);

    List<Student> findByDepartment(Department department);

    List<Student> findByCourse(Course course);

    List<Student> findBySection(com.example.studentmanagement.entity.Section section);

    List<Student> findBySectionId(Long sectionId);

    List<Student> findByCourseAndSemester(Course course, int semester);

    List<Student> findByCourseIdAndSemester(Long courseId, int semester);

    List<Student> findBySemester(int semester);

    @Query("SELECT s FROM Student s WHERE " +
           "(:search IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.studentId) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:deptId IS NULL OR s.department.id = :deptId) AND " +
           "(:courseId IS NULL OR s.course.id = :courseId) AND " +
           "(:semester IS NULL OR s.semester = :semester) AND " +
           "(:sectionId IS NULL OR (s.section IS NOT NULL AND s.section.id = :sectionId)) AND " +
           "(:status IS NULL OR s.accountStatus = :status)")
    List<Student> searchAndFilter(
            @Param("search") String search,
            @Param("deptId") Long deptId,
            @Param("courseId") Long courseId,
            @Param("semester") Integer semester,
            @Param("sectionId") Long sectionId,
            @Param("status") String status
    );

    default List<Student> searchAndFilter(String search, Long deptId, Long courseId, Integer semester, String status) {
        return searchAndFilter(search, deptId, courseId, semester, null, status);
    }

    List<Student> findTop5ByOrderByCreatedAtDesc();
    List<Student> findTop10ByOrderByCreatedAtDesc();
}
