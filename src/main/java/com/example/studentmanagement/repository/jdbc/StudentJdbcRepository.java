package com.example.studentmanagement.repository.jdbc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface defining Database Operations via direct JDBC.
 * Demonstrates Abstraction and Interface design principle in OOP.
 */
public interface StudentJdbcRepository {

    /**
     * Retrieves all student records with department and course information using JDBC.
     *
     * @return List of generic Key-Value records
     */
    List<Map<String, Object>> findAllStudentsJdbc();

    /**
     * Finds a student by their unique student_id using parameterized PreparedStatement.
     *
     * @param studentId Unique student identifier
     * @return Optional map of student attributes
     */
    Optional<Map<String, Object>> findByStudentIdJdbc(String studentId);

    /**
     * Counts the total number of enrolled students using JDBC aggregate query.
     *
     * @return total count
     */
    long countTotalStudentsJdbc();

    /**
     * Fetches department-wise student distribution using GROUP BY via JDBC.
     *
     * @return List of maps containing department name and student count
     */
    List<Map<String, Object>> getStudentDistributionByDepartmentJdbc();
}
