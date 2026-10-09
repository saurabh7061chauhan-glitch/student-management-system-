package com.example.studentmanagement.controller;

import com.example.studentmanagement.dto.*;
import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AcademicService academicService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private MarksService marksService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private com.example.studentmanagement.service.VerificationService verificationService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private AuditLogService auditLogService;

    // --- Students ---
    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<Student>>> getStudents(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "deptId", required = false) Long deptId,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "semester", required = false) Integer semester,
            @RequestParam(value = "status", required = false) String status) {
        List<Student> students = studentService.searchAndFilter(search, deptId, courseId, semester, status);
        return ResponseEntity.ok(ApiResponse.ok("Students retrieved successfully", students));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Student>> getStudentById(@PathVariable("id") Long id) {
        Student student = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Student found", student));
    }

    @PostMapping("/students")
    public ResponseEntity<ApiResponse<Student>> createStudent(@Valid @RequestBody StudentDTO dto) {
        Student created = studentService.saveStudent(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Student registered successfully", created));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Student>> updateStudent(@PathVariable("id") Long id,
                                                              @Valid @RequestBody StudentDTO dto) {
        Student updated = studentService.updateStudent(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Student updated successfully", updated));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable("id") Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.ok("Student deleted successfully"));
    }

    @PostMapping("/students/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStudentStatus(@PathVariable("id") Long id,
                                                                 @RequestBody Map<String, Boolean> payload) {
        boolean activate = payload.getOrDefault("activate", true);
        studentService.toggleStudentStatus(id, activate);
        return ResponseEntity.ok(ApiResponse.ok("Student status updated to " + (activate ? "Active" : "Disabled")));
    }

    @PostMapping("/students/{id}/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendStudentVerification(@PathVariable("id") Long id) {
        studentService.resendStudentVerification(id);
        return ResponseEntity.ok(ApiResponse.ok("Verification email dispatched for student."));
    }

    // --- Teachers ---
    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<List<Teacher>>> getTeachers() {
        return ResponseEntity.ok(ApiResponse.ok("Teachers retrieved", teacherService.getAllTeachers()));
    }

    @PostMapping("/teachers")
    public ResponseEntity<ApiResponse<Teacher>> createTeacher(@Valid @RequestBody TeacherDTO dto) {
        Teacher teacher = teacherService.saveTeacher(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Teacher created successfully", teacher));
    }

    @PutMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<Teacher>> updateTeacher(@PathVariable("id") Long id,
                                                              @Valid @RequestBody TeacherDTO dto) {
        Teacher teacher = teacherService.updateTeacher(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Teacher updated successfully", teacher));
    }

    @PatchMapping("/teachers/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateTeacherStatus(@PathVariable("id") Long id,
                                                                 @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Status is required"));
        }
        teacherService.updateAccountStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Faculty account status updated to " + status));
    }

    @PostMapping("/teachers/{id}/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendTeacherVerification(@PathVariable("id") Long id) {
        Teacher teacher = teacherService.getTeacherById(id);
        verificationService.resendVerification(teacher.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Verification email resent to " + teacher.getEmail()));
    }

    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(@PathVariable("id") Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.ok(ApiResponse.ok("Teacher deleted successfully"));
    }

    // --- Departments ---
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<Department>>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved", academicService.getAllDepartments()));
    }

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<Department>> createDepartment(@RequestBody Department dept) {
        Department created = academicService.saveDepartment(dept);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Department saved", created));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable("id") Long id) {
        academicService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully"));
    }

    // --- Courses ---
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<Course>>> getCourses() {
        return ResponseEntity.ok(ApiResponse.ok("Courses retrieved", academicService.getAllCourses()));
    }

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<Course>> createCourse(@RequestBody Course course) {
        Course created = academicService.saveCourse(course);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course saved", created));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable("id") Long id) {
        academicService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.ok("Course deleted successfully"));
    }

    // --- Subjects ---
    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<Subject>>> getSubjects() {
        return ResponseEntity.ok(ApiResponse.ok("Subjects retrieved", academicService.getAllSubjects()));
    }

    @PostMapping("/subjects")
    public ResponseEntity<ApiResponse<Subject>> createSubject(@RequestBody Subject subject) {
        Subject created = academicService.saveSubject(subject);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Subject saved", created));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable("id") Long id) {
        academicService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.ok("Subject deleted successfully"));
    }

    // --- Attendance ---
    @PostMapping("/attendance")
    public ResponseEntity<ApiResponse<Attendance>> markAttendance(@Valid @RequestBody AttendanceDTO dto, java.security.Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        Attendance record = attendanceService.saveAttendanceByAdmin(dto, username);
        return ResponseEntity.ok(ApiResponse.ok("Attendance marked successfully", record));
    }

    // --- Marks ---
    @PostMapping("/marks")
    public ResponseEntity<ApiResponse<Marks>> saveMarks(@Valid @RequestBody MarksDTO dto) {
        Marks record = marksService.saveOrUpdateMarks(dto);
        return ResponseEntity.ok(ApiResponse.ok("Marks saved successfully", record));
    }

    @DeleteMapping("/marks/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMarks(@PathVariable("id") Long id) {
        marksService.deleteMarks(id);
        return ResponseEntity.ok(ApiResponse.ok("Marks record deleted"));
    }

    // --- Fees ---
    @PostMapping("/fees")
    public ResponseEntity<ApiResponse<Fee>> saveFee(@Valid @RequestBody FeeDTO dto) {
        Fee record = feeService.saveOrUpdateFee(dto);
        return ResponseEntity.ok(ApiResponse.ok("Fee recorded successfully", record));
    }

    @DeleteMapping("/fees/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFee(@PathVariable("id") Long id) {
        feeService.deleteFee(id);
        return ResponseEntity.ok(ApiResponse.ok("Fee record deleted"));
    }

    // --- Notices ---
    @PostMapping("/notices")
    public ResponseEntity<ApiResponse<Notice>> saveNotice(@RequestBody Notice notice) {
        Notice created = noticeService.saveNotice(notice);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Notice published successfully", created));
    }

    @DeleteMapping("/notices/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable("id") Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(ApiResponse.ok("Notice removed successfully"));
    }

    // --- Global Search ---
    @GetMapping("/global-search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> globalSearch(@RequestParam(value = "q", defaultValue = "") String query) {
        String q = query.trim().toLowerCase();
        Map<String, Object> results = new java.util.LinkedHashMap<>();

        if (q.isEmpty()) {
            results.put("students", List.of());
            results.put("faculty", List.of());
            results.put("courses", List.of());
            results.put("departments", List.of());
            results.put("subjects", List.of());
            return ResponseEntity.ok(ApiResponse.ok("Empty query", results));
        }

        // Students (match rollNo, name, email)
        List<Map<String, Object>> students = studentService.getAllStudents().stream()
                .filter(s -> (s.getRollNo() != null && s.getRollNo().toLowerCase().contains(q)) ||
                             (s.getFullName() != null && s.getFullName().toLowerCase().contains(q)) ||
                             (s.getEmail() != null && s.getEmail().toLowerCase().contains(q)))
                .limit(8)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("title", s.getFullName() + " (" + s.getRollNo() + ")");
                    m.put("subtitle", (s.getDepartment() != null ? s.getDepartment().getCode() : "") + " - " + (s.getCourse() != null ? s.getCourse().getCode() : ""));
                    m.put("url", "/admin/students/" + s.getId());
                    m.put("type", "Student");
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        // Faculty (match employeeId, name, email)
        List<Map<String, Object>> faculty = teacherService.getAllTeachers().stream()
                .filter(t -> (t.getEmployeeId() != null && t.getEmployeeId().toLowerCase().contains(q)) ||
                             (t.getFullName() != null && t.getFullName().toLowerCase().contains(q)) ||
                             (t.getEmail() != null && t.getEmail().toLowerCase().contains(q)))
                .limit(8)
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("title", t.getFullName() + " (" + t.getEmployeeId() + ")");
                    m.put("subtitle", (t.getDepartment() != null ? t.getDepartment().getCode() : "") + " - " + (t.getDesignation() != null ? t.getDesignation() : ""));
                    m.put("url", "/admin/teachers?search=" + t.getEmployeeId());
                    m.put("type", "Faculty");
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        // Courses (match code, name)
        List<Map<String, Object>> courses = academicService.getAllCourses().stream()
                .filter(c -> (c.getCode() != null && c.getCode().toLowerCase().contains(q)) ||
                             (c.getName() != null && c.getName().toLowerCase().contains(q)))
                .limit(5)
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("title", c.getName() + " (" + c.getCode() + ")");
                    m.put("subtitle", "Course");
                    m.put("url", "/admin/academics");
                    m.put("type", "Course");
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        // Departments (match code, name)
        List<Map<String, Object>> departments = academicService.getAllDepartments().stream()
                .filter(d -> (d.getCode() != null && d.getCode().toLowerCase().contains(q)) ||
                             (d.getName() != null && d.getName().toLowerCase().contains(q)))
                .limit(5)
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", d.getId());
                    m.put("title", d.getName() + " (" + d.getCode() + ")");
                    m.put("subtitle", "Department");
                    m.put("url", "/admin/academics");
                    m.put("type", "Department");
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        // Subjects (match code, name)
        List<Map<String, Object>> subjects = academicService.getAllSubjects().stream()
                .filter(sb -> (sb.getCode() != null && sb.getCode().toLowerCase().contains(q)) ||
                              (sb.getName() != null && sb.getName().toLowerCase().contains(q)))
                .limit(5)
                .map(sb -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", sb.getId());
                    m.put("title", sb.getName() + " (" + sb.getCode() + ")");
                    m.put("subtitle", "Subject - Sem " + sb.getSemester());
                    m.put("url", "/admin/academics");
                    m.put("type", "Subject");
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        results.put("students", students);
        results.put("faculty", faculty);
        results.put("courses", courses);
        results.put("departments", departments);
        results.put("subjects", subjects);

        return ResponseEntity.ok(ApiResponse.ok("Search results found", results));
    }

    // --- Sections API ---
    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<List<Section>>> getSections() {
        return ResponseEntity.ok(ApiResponse.ok("Sections retrieved", sectionService.getAllSections()));
    }

    @PostMapping("/sections")
    public ResponseEntity<ApiResponse<Section>> createSectionApi(@RequestBody Section section) {
        Section saved = sectionService.saveSection(section);
        auditLogService.logAction("ADMIN", "CREATE_SECTION", "SECTION", saved.getId(), "Created section " + saved.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Section created", saved));
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSectionApi(@PathVariable("id") Long id) {
        sectionService.deleteSection(id);
        auditLogService.logAction("ADMIN", "DELETE_SECTION", "SECTION", id, "Deleted section id " + id);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted"));
    }
}
