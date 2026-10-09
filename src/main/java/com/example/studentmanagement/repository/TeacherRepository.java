package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Department;
import com.example.studentmanagement.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByTeacherId(String teacherId);

    Optional<Teacher> findByTeacherIdIgnoreCase(String teacherId);

    Optional<Teacher> findByEmail(String email);

    Optional<Teacher> findByEmailIgnoreCase(String email);

    boolean existsByTeacherId(String teacherId);

    boolean existsByEmail(String email);

    Optional<Teacher> findByUser(com.example.studentmanagement.entity.User user);

    Optional<Teacher> findByUserUsername(String username);

    List<Teacher> findByDepartment(Department department);

    @Query("SELECT t FROM Teacher t WHERE " +
           "(:query IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.teacherId) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:deptId IS NULL OR t.department.id = :deptId)")
    List<Teacher> searchTeachers(@Param("query") String query, @Param("deptId") Long deptId);
}
