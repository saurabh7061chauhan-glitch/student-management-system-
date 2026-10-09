package com.example.studentmanagement.controller;

import com.example.studentmanagement.dto.*;
import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty")
public class ApiFacultyController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private MarksService marksService;

    @Autowired
    private QrAttendanceService qrAttendanceService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private StudyMaterialService studyMaterialService;

    private Teacher getAuthenticatedTeacher(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User not authenticated.");
        }
        return teacherService.getTeacherByUsername(principal.getName());
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Student>> getStudentDetails(@PathVariable("id") Long id) {
        Student student = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Student details retrieved", student));
    }

    @PostMapping("/attendance")
    public ResponseEntity<ApiResponse<Void>> markAttendance(
            Principal principal,
            @Valid @RequestBody AttendanceDTO dto) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        attendanceService.markAttendanceForFaculty(dto, teacher);
        return ResponseEntity.ok(ApiResponse.ok("Attendance recorded successfully"));
    }

    @PostMapping("/attendance/batch")
    public ResponseEntity<ApiResponse<Void>> markBatchAttendance(
            Principal principal,
            @Valid @RequestBody List<AttendanceDTO> dtoList) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        attendanceService.markBatchAttendanceForFaculty(dtoList, teacher);
        return ResponseEntity.ok(ApiResponse.ok("Batch attendance recorded successfully"));
    }

    @PostMapping("/marks")
    public ResponseEntity<ApiResponse<Marks>> saveMarks(
            Principal principal,
            @Valid @RequestBody MarksDTO dto) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        Marks saved = marksService.saveOrUpdateMarksForFaculty(dto, teacher);
        return ResponseEntity.ok(ApiResponse.ok("Marks saved successfully", saved));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            Principal principal,
            @RequestBody Map<String, String> payload) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        String phone = payload.get("phone");
        String photo = payload.get("profileImage");
        teacherService.updateFacultyProfile(teacher, phone, photo);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Principal principal,
            @Valid @RequestBody PasswordChangeRequest request) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New passwords do not match"));
        }
        teacherService.changeFacultyPassword(teacher, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    // --- QR Attendance APIs ---
    @PostMapping("/qr-attendance/start")
    public ResponseEntity<ApiResponse<com.example.studentmanagement.entity.QrAttendanceSession>> startQrAttendance(
            Principal principal,
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestParam(value = "subjectId", required = false) Long paramSubjectId,
            @RequestParam(value = "sectionId", required = false) Long paramSectionId,
            @RequestParam(value = "validMinutes", required = false) Integer paramValidMinutes) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        Long subjectId = paramSubjectId;
        Long sectionId = paramSectionId;
        int validMinutes = paramValidMinutes != null ? paramValidMinutes : 10;

        if (payload != null) {
            if (payload.get("subjectId") != null) {
                subjectId = Long.valueOf(payload.get("subjectId").toString());
            }
            if (payload.get("sectionId") != null) {
                sectionId = Long.valueOf(payload.get("sectionId").toString());
            }
            if (payload.get("validMinutes") != null) {
                validMinutes = Integer.parseInt(payload.get("validMinutes").toString());
            }
        }

        if (subjectId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subject ID is required"));
        }

        com.example.studentmanagement.entity.QrAttendanceSession session =
                qrAttendanceService.startSession(teacher, subjectId, sectionId, validMinutes);
        return ResponseEntity.ok(ApiResponse.ok("QR Attendance session started", session));
    }

    @PostMapping("/qr-attendance/stop/{id}")
    public ResponseEntity<ApiResponse<Void>> stopQrAttendance(
            Principal principal,
            @PathVariable("id") Long sessionId) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        qrAttendanceService.stopSession(sessionId, teacher);
        return ResponseEntity.ok(ApiResponse.ok("QR Attendance session stopped"));
    }

    @PostMapping("/qr-attendance/stop")
    public ResponseEntity<ApiResponse<Void>> stopQrAttendanceGeneric(
            Principal principal,
            @RequestParam(value = "subjectId", required = false) Long subjectId) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        qrAttendanceService.getActiveSessionForTeacher(teacher).ifPresent(s -> {
            qrAttendanceService.stopSession(s.getId(), teacher);
        });
        return ResponseEntity.ok(ApiResponse.ok("QR Attendance session stopped"));
    }

    @GetMapping("/qr-attendance/active")
    public ResponseEntity<ApiResponse<com.example.studentmanagement.entity.QrAttendanceSession>> getActiveQrSession(
            Principal principal) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        com.example.studentmanagement.entity.QrAttendanceSession session =
                qrAttendanceService.getActiveSessionForTeacher(teacher).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok("Active QR session retrieved", session));
    }

    // --- Assignment APIs ---
    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.Assignment>>> getAssignments(
            Principal principal) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        return ResponseEntity.ok(ApiResponse.ok("Assignments retrieved", assignmentService.getAssignmentsByTeacher(teacher)));
    }

    // --- Study Material APIs ---
    @GetMapping("/study-materials")
    public ResponseEntity<ApiResponse<List<com.example.studentmanagement.entity.StudyMaterial>>> getStudyMaterials(
            Principal principal) {
        Teacher teacher = getAuthenticatedTeacher(principal);
        return ResponseEntity.ok(ApiResponse.ok("Study materials retrieved", studyMaterialService.getMaterialsByTeacher(teacher)));
    }
}
