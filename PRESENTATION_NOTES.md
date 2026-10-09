# 🎙️ Review 1: Presentation & Viva Preparation Guide

This guide gives you the exact speaking points, rubric mapping, and anticipated examiner questions for **Academic Review 1**.

---

## 🎯 Quick Rubric Summary & What To Say

### 1. Problem Understanding & Solution Design (8 Marks)
* **What the panel wants to hear:**
  > *"Our project solves the fragmentation in college administrative workflows. Traditional systems rely on scattered registers, paper fees, and isolated spreadsheets. We have designed a unified 3-tier ERP architecture that connects Administrators, Faculty, and Students in real time, featuring QR-code attendance, automatic GPA calculation, and instant PDF receipts."*

### 2. Core Java Concepts (10 Marks)
* **What the panel wants to hear:**
  > *"We used Java 17 and strictly implemented Object-Oriented Programming (OOP) principles:*
  > * **Abstraction & Interfaces:** Demonstrated in `StudentJdbcRepository` and Spring Data interfaces.
  > * **Polymorphism:** Concrete implementation `StudentJdbcRepositoryImpl` overriding interface behaviors.
  > * **Exception Handling:** Centralized `@ControllerAdvice` in `GlobalExceptionHandler.java` capturing resource errors and validating input DTOs.
  > * **Collections & Generics:** Extensive use of `List<StudentDTO>`, `Map<String, Object>`, and Java 8+ Streams API for filtering and processing records."*

### 3. Database Integration (JDBC) (8 Marks)
* **What the panel wants to hear:**
  > *"For data persistence, we built a dual-layer architecture:*
  > * 1. **Spring Data JPA & Hibernate** on top of **HikariCP** connection pooling for object-relational mapping across 28 entities.
  > * 2. **Explicit JDBC Layer:** We created `JdbcDatabaseConnection.java` and `StudentJdbcRepositoryImpl.java` utilizing raw `java.sql.Connection`, `PreparedStatement` (for SQL injection security), and `ResultSet`.
  > * 3. We also created a live diagnostic endpoint at `/api/diagnostics/jdbc/status` that verifies real-time JDBC connectivity and database metadata."*

### 4. Servlets & Web Integration (7 Marks)
* **What the panel wants to hear:**
  > *"Our web layer is built upon the Java Servlet specification:*
  > * Incoming requests are handled by Spring MVC's `DispatcherServlet` following the Front Controller architectural pattern.
  > * We manage user sessions, Spring Security filters, multipart file uploads, and Thymeleaf view resolution seamlessly.
  > * We also provide RESTful JSON endpoints under `/api/**` for external/mobile integration."*

---

## 🖥️ Live Demonstration Checklist

Have these tabs ready in your browser during the review:

1. **Admin Portal**:
   * URL: `http://localhost:8080/login`
   * Username: `admin` | Password: `Admin@123`
   * Show: Admin Dashboard analytics, Course & Department list, Timetable matrix.

2. **Faculty Portal**:
   * Username: `david.miller@college.edu` | Password: `Faculty@123`
   * Show: Dynamic QR Code generation for attendance, Marksheet entry.

3. **Student Portal**:
   * Username: `alex.morgan@student.edu` | Password: `Student@123`
   * Show: Student Dashboard, Live attendance percentage, Marksheet PDF download.

4. **Direct JDBC Diagnostics**:
   * URL: `http://localhost:8080/api/diagnostics/jdbc/status`
   * Show: Demonstrates live JDBC driver, catalog, and query results via `PreparedStatement`.

---

## ❓ Frequently Asked Viva Questions & Answers

#### Q1: "Where is JDBC implemented in your code?"
* **Answer:** *"Sir/Ma'am, in `com.example.studentmanagement.config.JdbcDatabaseConnection`, we obtain JDBC connections via `DriverManager` and pooled `DataSource`. In `com.example.studentmanagement.repository.jdbc.StudentJdbcRepositoryImpl`, we execute queries using `PreparedStatement` and read the `ResultSet` directly into Collections."*

#### Q2: "How is your application responsive?"
* **Answer:** *"We use Bootstrap 5.3.3 responsive grid classes (`col-12`, `col-md-6`, `col-lg-4`), collapsible offcanvas sidebars for mobile devices, and mobile-friendly tables with custom CSS in `style.css`."*

#### Q3: "How do you prevent SQL Injection?"
* **Answer:** *"In our JDBC repository, we strictly use parameterized `PreparedStatement` with placeholders (`?`) instead of string concatenation. In our ORM layer, Hibernate uses parameterized HQL/JPQL."*

#### Q4: "How does Servlet integration work here?"
* **Answer:** *"Spring Boot runs on an embedded Apache Tomcat servlet container. The core servlet is `DispatcherServlet`, which receives every HTTP request, checks security filters, routes to our `@Controller` methods, and passes model data to Thymeleaf for rendering."*
