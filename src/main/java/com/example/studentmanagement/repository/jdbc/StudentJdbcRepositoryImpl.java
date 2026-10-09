package com.example.studentmanagement.repository.jdbc;

import com.example.studentmanagement.config.JdbcDatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Concrete implementation of Student database operations using raw JDBC.
 * Demonstrates:
 * - OOP: Polymorphism, Inheritance (implements interface), Encapsulation
 * - JDBC: Connection, PreparedStatement (SQL injection prevention), ResultSet
 * - Collections & Generics: List<Map<String, Object>>, ArrayList, HashMap
 * - Exception Handling: Try-with-resources, SQLException handling and propagation
 */
@Repository
public class StudentJdbcRepositoryImpl implements StudentJdbcRepository {

    private static final Logger logger = LoggerFactory.getLogger(StudentJdbcRepositoryImpl.class);

    private final JdbcDatabaseConnection databaseConnection;

    public StudentJdbcRepositoryImpl(JdbcDatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public List<Map<String, Object>> findAllStudentsJdbc() {
        List<Map<String, Object>> students = new ArrayList<>();
        String sql = "SELECT s.id, s.student_id, s.full_name, s.email, s.phone, s.semester, s.account_status, " +
                     "d.name AS department_name, c.course_name AS course_name " +
                     "FROM students s " +
                     "LEFT JOIN departments d ON s.department_id = d.id " +
                     "LEFT JOIN courses c ON s.course_id = c.id " +
                     "ORDER BY s.id DESC";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("studentId", rs.getString("student_id"));
                row.put("fullName", rs.getString("full_name"));
                row.put("email", rs.getString("email"));
                row.put("phone", rs.getString("phone"));
                row.put("semester", rs.getInt("semester"));
                row.put("accountStatus", rs.getString("account_status"));
                row.put("departmentName", rs.getString("department_name"));
                row.put("courseName", rs.getString("course_name"));
                students.add(row);
            }
            logger.info("JDBC: Successfully fetched {} students.", students.size());
        } catch (SQLException e) {
            logger.error("JDBC Error in findAllStudentsJdbc: {}", e.getMessage(), e);
        }
        return students;
    }

    @Override
    public Optional<Map<String, Object>> findByStudentIdJdbc(String studentId) {
        String sql = "SELECT s.id, s.student_id, s.full_name, s.email, s.phone, s.semester, s.account_status, " +
                     "d.name AS department_name, c.course_name AS course_name " +
                     "FROM students s " +
                     "LEFT JOIN departments d ON s.department_id = d.id " +
                     "LEFT JOIN courses c ON s.course_id = c.id " +
                     "WHERE s.student_id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("studentId", rs.getString("student_id"));
                    row.put("fullName", rs.getString("full_name"));
                    row.put("email", rs.getString("email"));
                    row.put("phone", rs.getString("phone"));
                    row.put("semester", rs.getInt("semester"));
                    row.put("accountStatus", rs.getString("account_status"));
                    row.put("departmentName", rs.getString("department_name"));
                    row.put("courseName", rs.getString("course_name"));
                    return Optional.of(row);
                }
            }
        } catch (SQLException e) {
            logger.error("JDBC Error in findByStudentIdJdbc for ID {}: {}", studentId, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public long countTotalStudentsJdbc() {
        String sql = "SELECT COUNT(*) AS total_count FROM students";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong("total_count");
            }
        } catch (SQLException e) {
            logger.error("JDBC Error in countTotalStudentsJdbc: {}", e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Map<String, Object>> getStudentDistributionByDepartmentJdbc() {
        List<Map<String, Object>> distribution = new ArrayList<>();
        String sql = "SELECT COALESCE(d.name, 'Unassigned') AS dept_name, COUNT(s.id) AS student_count " +
                     "FROM students s " +
                     "LEFT JOIN departments d ON s.department_id = d.id " +
                     "GROUP BY d.name " +
                     "ORDER BY student_count DESC";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("department", rs.getString("dept_name"));
                map.put("count", rs.getLong("student_count"));
                distribution.add(map);
            }
        } catch (SQLException e) {
            logger.error("JDBC Error in getStudentDistributionByDepartmentJdbc: {}", e.getMessage(), e);
        }
        return distribution;
    }
}
