package com.example.studentmanagement.repository;

import com.example.studentmanagement.entity.Fee;
import com.example.studentmanagement.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    List<Fee> findByStudentOrderBySemesterAsc(Student student);

    List<Fee> findByStudentIdOrderBySemesterAsc(Long studentId);

    List<Fee> findByPaymentStatus(String paymentStatus);

    @Query("SELECT COALESCE(SUM(f.totalAmount), 0.0) FROM Fee f")
    Double sumTotalFees();

    @Query("SELECT COALESCE(SUM(f.paidAmount), 0.0) FROM Fee f")
    Double sumPaidFees();

    @Query("SELECT COALESCE(SUM(f.remainingAmount), 0.0) FROM Fee f")
    Double sumRemainingFees();

    long countByPaymentStatus(String paymentStatus);
}
