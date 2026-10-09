package com.example.studentmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "book_issues")
public class BookIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "borrower_type", nullable = false, length = 20)
    private String borrowerType = "STUDENT"; // STUDENT, FACULTY

    @Column(name = "borrower_name", nullable = false, length = 120)
    private String borrowerName;

    @Column(name = "borrower_identifier", nullable = false, length = 60)
    private String borrowerIdentifier;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate = LocalDate.now();

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "fine_amount", nullable = false)
    private double fineAmount = 0.0;

    @Column(name = "fine_paid", nullable = false)
    private boolean finePaid = false;

    @Column(nullable = false, length = 30)
    private String status = "ISSUED"; // ISSUED, RETURNED, OVERDUE, LOST

    @Column(length = 255)
    private String remarks;

    @Column(name = "issued_by", length = 100)
    private String issuedBy = "Admin";

    public BookIssue() {
    }

    public BookIssue(Book book, Student student, LocalDate issueDate, LocalDate dueDate, String issuedBy) {
        this.book = book;
        this.student = student;
        this.borrowerType = "STUDENT";
        this.borrowerName = student.getFullName();
        this.borrowerIdentifier = student.getStudentId();
        this.issueDate = issueDate != null ? issueDate : LocalDate.now();
        this.dueDate = dueDate != null ? dueDate : this.issueDate.plusDays(14);
        this.issuedBy = issuedBy;
        this.status = "ISSUED";
        this.fineAmount = 0.0;
        this.finePaid = false;
    }

    public BookIssue(Book book, Teacher teacher, LocalDate issueDate, LocalDate dueDate, String issuedBy) {
        this.book = book;
        this.teacher = teacher;
        this.borrowerType = "FACULTY";
        this.borrowerName = teacher.getName();
        this.borrowerIdentifier = teacher.getTeacherId();
        this.issueDate = issueDate != null ? issueDate : LocalDate.now();
        this.dueDate = dueDate != null ? dueDate : this.issueDate.plusDays(30);
        this.issuedBy = issuedBy;
        this.status = "ISSUED";
        this.fineAmount = 0.0;
        this.finePaid = false;
    }

    public void updateOverdueFine() {
        if ("RETURNED".equalsIgnoreCase(this.status)) {
            if (this.returnDate != null && this.dueDate != null && this.returnDate.isAfter(this.dueDate)) {
                long days = ChronoUnit.DAYS.between(this.dueDate, this.returnDate);
                this.fineAmount = days * 1.0;
            }
            return;
        }

        if (this.dueDate != null && LocalDate.now().isAfter(this.dueDate)) {
            long days = ChronoUnit.DAYS.between(this.dueDate, LocalDate.now());
            this.fineAmount = days * 1.0;
            this.status = "OVERDUE";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public String getBorrowerType() {
        return borrowerType;
    }

    public void setBorrowerType(String borrowerType) {
        this.borrowerType = borrowerType;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public String getBorrowerIdentifier() {
        return borrowerIdentifier;
    }

    public void setBorrowerIdentifier(String borrowerIdentifier) {
        this.borrowerIdentifier = borrowerIdentifier;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public boolean isFinePaid() {
        return finePaid;
    }

    public void setFinePaid(boolean finePaid) {
        this.finePaid = finePaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }
}
