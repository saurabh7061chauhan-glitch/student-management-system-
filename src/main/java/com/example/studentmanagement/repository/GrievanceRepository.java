package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Grievance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long> {
    List<Grievance> findAllByOrderByCreatedAtDesc();
    List<Grievance> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Grievance> findByAssignedTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<Grievance> findByStatus(String status);
    Optional<Grievance> findByTicketNumber(String ticketNumber);
    long countByStatus(String status);

    @Query("SELECT g FROM Grievance g WHERE " +
           "(:category IS NULL OR :category = '' OR g.category = :category) AND " +
           "(:status IS NULL OR :status = '' OR g.status = :status) " +
           "ORDER BY g.createdAt DESC")
    List<Grievance> filterGrievances(@Param("category") String category, @Param("status") String status);
}
