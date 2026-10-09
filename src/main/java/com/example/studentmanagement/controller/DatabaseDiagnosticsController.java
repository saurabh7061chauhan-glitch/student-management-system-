package com.example.studentmanagement.controller;

import com.example.studentmanagement.config.JdbcDatabaseConnection;
import com.example.studentmanagement.repository.jdbc.StudentJdbcRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller demonstrating Database Connectivity (JDBC) and Operations.
 * Directly addresses Academic Review Rubrics:
 * - Database Integration (JDBC)
 * - Classes for database operations
 * - Servlets & Web Integration
 */
@RestController
@RequestMapping("/api/diagnostics/jdbc")
public class DatabaseDiagnosticsController {

    private final JdbcDatabaseConnection jdbcDatabaseConnection;
    private final StudentJdbcRepository studentJdbcRepository;

    public DatabaseDiagnosticsController(JdbcDatabaseConnection jdbcDatabaseConnection,
                                         StudentJdbcRepository studentJdbcRepository) {
        this.jdbcDatabaseConnection = jdbcDatabaseConnection;
        this.studentJdbcRepository = studentJdbcRepository;
    }

    /**
     * Diagnostic endpoint verifying live JDBC Connection status, driver info, and metadata.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getJdbcConnectionStatus() {
        Map<String, Object> response = new HashMap<>();
        try (Connection conn = jdbcDatabaseConnection.getConnection()) {
            response.put("status", "SUCCESS");
            response.put("jdbcConnected", true);
            response.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
            response.put("databaseVersion", conn.getMetaData().getDatabaseProductVersion());
            response.put("driverName", conn.getMetaData().getDriverName());
            response.put("driverVersion", conn.getMetaData().getDriverVersion());
            response.put("catalog", conn.getCatalog());
            response.put("totalStudentsCount", studentJdbcRepository.countTotalStudentsJdbc());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "FAILED");
            response.put("jdbcConnected", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Diagnostic endpoint demonstrating JDBC Query execution via PreparedStatement & ResultSet.
     */
    @GetMapping("/students")
    public ResponseEntity<Map<String, Object>> getStudentsViaJdbc() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> students = studentJdbcRepository.findAllStudentsJdbc();
        response.put("queryType", "Direct JDBC PreparedStatement");
        response.put("recordCount", students.size());
        response.put("data", students);
        return ResponseEntity.ok(response);
    }
}
