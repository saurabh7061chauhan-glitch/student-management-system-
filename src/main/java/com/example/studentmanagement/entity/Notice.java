package com.example.studentmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String category = "General"; // Exam, Attendance, Fee, General, Result

    @Column(name = "target_audience", nullable = false, length = 50)
    private String targetAudience = "ALL"; // ALL, STUDENT, TEACHER, DEPARTMENT, COURSE, SEMESTER, SECTION, INDIVIDUAL

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "semester")
    private Integer semester;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(length = 100)
    private String sender = "Admin";

    @Column(name = "posted_date", nullable = false)
    private LocalDate postedDate = LocalDate.now();

    @Column(length = 20)
    private String priority = "Normal"; // Normal, High, Urgent

    public Notice() {
    }

    public Notice(String title, String content, String category, String targetAudience, LocalDate postedDate, String priority) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.targetAudience = targetAudience;
        this.postedDate = postedDate != null ? postedDate : LocalDate.now();
        this.priority = priority != null ? priority : "Normal";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getTargetRole() {
        return targetAudience;
    }

    public void setTargetRole(String targetRole) {
        this.targetAudience = targetRole;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(LocalDate postedDate) {
        this.postedDate = postedDate;
    }

    public LocalDate getPublishDate() {
        return postedDate;
    }

    public void setPublishDate(LocalDate publishDate) {
        this.postedDate = publishDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
