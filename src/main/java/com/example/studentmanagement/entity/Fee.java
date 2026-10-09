package com.example.studentmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "fees")
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "fee_title", nullable = false, length = 100)
    private String feeTitle;

    @Column(nullable = false)
    private int semester = 1;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount = 0.0;

    @Column(name = "paid_amount", nullable = false)
    private double paidAmount = 0.0;

    @Column(name = "remaining_amount", nullable = false)
    private double remainingAmount = 0.0;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "Pending"; // Paid, Partial, Pending

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // Online, Cash, Bank Transfer, Cheque

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "academic_year", length = 30)
    private String academicYear = "2025-2026";

    @Column(length = 255)
    private String remarks;

    public Fee() {
    }

    public Fee(Student student, String feeTitle, int semester, double totalAmount, double paidAmount, LocalDate paymentDate, String paymentMethod, String transactionId) {
        this.student = student;
        this.feeTitle = feeTitle;
        this.semester = semester;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        calculateStatus();
    }

    public void calculateStatus() {
        this.remainingAmount = Math.max(0.0, this.totalAmount - this.paidAmount);
        if (this.paidAmount <= 0) {
            this.paymentStatus = "Pending";
        } else if (this.paidAmount >= this.totalAmount) {
            this.paymentStatus = "Paid";
        } else {
            this.paymentStatus = "Partial";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getFeeTitle() {
        return feeTitle;
    }

    public void setFeeTitle(String feeTitle) {
        this.feeTitle = feeTitle;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
        calculateStatus();
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
        calculateStatus();
    }

    public double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTitle() {
        return feeTitle;
    }

    public void setTitle(String title) {
        this.feeTitle = title;
    }

    public String getStatus() {
        return paymentStatus;
    }

    public void setStatus(String status) {
        this.paymentStatus = status;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getReceiptNumber() {
        return "REC-" + (paymentDate != null ? paymentDate.getYear() : "2025") + "-" + String.format("%05d", id != null ? id : 0);
    }
}
