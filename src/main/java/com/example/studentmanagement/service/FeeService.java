package com.example.studentmanagement.service;

import com.example.studentmanagement.dto.FeeDTO;
import com.example.studentmanagement.entity.Fee;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.FeeRepository;
import com.example.studentmanagement.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FeeService {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private StudentRepository studentRepository;

    public List<Fee> getAllFees() {
        return feeRepository.findAll();
    }

    public List<Fee> getFeesByStudent(Long studentId) {
        return feeRepository.findByStudentIdOrderBySemesterAsc(studentId);
    }

    public Fee getFeeById(Long id) {
        return feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found: " + id));
    }

    @Transactional
    public Fee saveOrUpdateFee(FeeDTO dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + dto.getStudentId()));

        Fee fee;
        if (dto.getId() != null) {
            fee = getFeeById(dto.getId());
        } else {
            fee = new Fee();
            fee.setStudent(student);
        }

        fee.setFeeTitle(dto.getFeeTitle());
        fee.setSemester(dto.getSemester());
        fee.setTotalAmount(dto.getTotalAmount());
        fee.setPaidAmount(dto.getPaidAmount());
        fee.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now());
        fee.setPaymentMethod(dto.getPaymentMethod());
        fee.setTransactionId(dto.getTransactionId());
        fee.calculateStatus();

        return feeRepository.save(fee);
    }

    @Transactional
    public void deleteFee(Long id) {
        if (!feeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fee record not found: " + id);
        }
        feeRepository.deleteById(id);
    }

    public double getTotalFeesSum() {
        Double val = feeRepository.sumTotalFees();
        return val != null ? val : 0.0;
    }

    public double getPaidFeesSum() {
        Double val = feeRepository.sumPaidFees();
        return val != null ? val : 0.0;
    }

    public double getRemainingFeesSum() {
        Double val = feeRepository.sumRemainingFees();
        return val != null ? val : 0.0;
    }

    public long getCountByStatus(String status) {
        return feeRepository.countByPaymentStatus(status);
    }
}
