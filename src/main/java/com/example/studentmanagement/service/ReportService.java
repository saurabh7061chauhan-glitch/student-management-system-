package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.*;
import com.example.studentmanagement.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ExaminationRepository examinationRepository;

    @Autowired
    private ExamScheduleRepository examScheduleRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookIssueRepository bookIssueRepository;

    @Autowired
    private GrievanceRepository grievanceRepository;

    public Map<String, Object> getOverallAnalytics(Long departmentId, Long courseId, Integer semester) {
        Map<String, Object> data = new HashMap<>();

        // 1. Students Analytics
        List<Student> allStudents = studentRepository.findAll();
        List<Student> filteredStudents = allStudents.stream()
                .filter(s -> departmentId == null || (s.getDepartment() != null && s.getDepartment().getId().equals(departmentId)))
                .filter(s -> courseId == null || (s.getCourse() != null && s.getCourse().getId().equals(courseId)))
                .filter(s -> semester == null || s.getSemester() == semester)
                .collect(Collectors.toList());

        data.put("totalStudents", filteredStudents.size());
        data.put("departmentStudentCounts", filteredStudents.stream()
                .collect(Collectors.groupingBy((Student s) -> s.getDepartment() != null ? s.getDepartment().getName() : "Unassigned", Collectors.counting())));
        data.put("courseStudentCounts", filteredStudents.stream()
                .collect(Collectors.groupingBy((Student s) -> s.getCourse() != null ? s.getCourse().getCourseName() : "General", Collectors.counting())));
        data.put("semesterStudentCounts", filteredStudents.stream()
                .collect(Collectors.groupingBy(Student::getSemester, TreeMap::new, Collectors.counting())));

        // 2. Attendance Analytics
        List<Attendance> allAttendance = attendanceRepository.findAll();
        long presentCount = allAttendance.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long totalAtt = allAttendance.size();
        double overallAttRate = totalAtt > 0 ? Math.round((presentCount * 100.0 / totalAtt) * 10.0) / 10.0 : 85.0;
        data.put("overallAttendanceRate", overallAttRate);
        data.put("totalAttendanceRecords", totalAtt);

        // Low attendance detection (< 75%)
        List<Map<String, Object>> lowAttendanceList = new ArrayList<>();
        for (Student st : filteredStudents) {
            long totalClasses = attendanceRepository.countByStudent(st);
            if (totalClasses > 0) {
                long pres = attendanceRepository.countByStudentAndStatus(st, "PRESENT");
                double pct = Math.round((pres * 100.0 / totalClasses) * 10.0) / 10.0;
                if (pct < 75.0) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("student", st);
                    map.put("percentage", pct);
                    map.put("totalClasses", totalClasses);
                    lowAttendanceList.add(map);
                }
            }
        }
        data.put("lowAttendanceStudents", lowAttendanceList);
        data.put("lowAttendanceCount", lowAttendanceList.size());

        // 3. Academic & Marks Analytics
        List<Marks> allMarks = marksRepository.findAll();
        List<Marks> filteredMarks = allMarks.stream()
                .filter(m -> departmentId == null || (m.getStudent().getDepartment() != null && m.getStudent().getDepartment().getId().equals(departmentId)))
                .filter(m -> courseId == null || (m.getStudent().getCourse() != null && m.getStudent().getCourse().getId().equals(courseId)))
                .filter(m -> semester == null || m.getSemester() == semester)
                .collect(Collectors.toList());

        long passMarksCount = filteredMarks.stream().filter(m -> "Pass".equalsIgnoreCase(m.getStatus())).count();
        double passPercentage = filteredMarks.isEmpty() ? 0.0 : Math.round((passMarksCount * 100.0 / filteredMarks.size()) * 10.0) / 10.0;
        double failPercentage = filteredMarks.isEmpty() ? 0.0 : Math.round(((filteredMarks.size() - passMarksCount) * 100.0 / filteredMarks.size()) * 10.0) / 10.0;
        double avgScore = filteredMarks.isEmpty() ? 0.0 : Math.round(filteredMarks.stream().mapToDouble(Marks::getPercentage).average().orElse(0.0) * 10.0) / 10.0;

        Map<String, Long> gradeDist = filteredMarks.stream()
                .collect(Collectors.groupingBy(Marks::getGrade, TreeMap::new, Collectors.counting()));

        data.put("averageScore", avgScore);
        data.put("passPercentage", passPercentage);
        data.put("failPercentage", failPercentage);
        data.put("gradeDistribution", gradeDist);
        data.put("totalEvaluatedPapers", filteredMarks.size());

        // 4. Finance & Fee Analytics
        List<Fee> fees = feeRepository.findAll();
        double totalFees = fees.stream().mapToDouble(Fee::getTotalAmount).sum();
        double collectedFees = fees.stream().mapToDouble(Fee::getPaidAmount).sum();
        double pendingFees = totalFees - collectedFees;
        double feeCollectionRate = totalFees > 0 ? Math.round((collectedFees / totalFees * 100.0) * 10.0) / 10.0 : 0.0;

        data.put("totalFeesBilled", totalFees);
        data.put("collectedFees", collectedFees);
        data.put("pendingFees", pendingFees);
        data.put("feeCollectionRate", feeCollectionRate);
        data.put("feeStatusDistribution", fees.stream().collect(Collectors.groupingBy(Fee::getPaymentStatus, Collectors.counting())));

        // 5. Examinations Analytics
        data.put("totalExaminations", examinationRepository.count());
        data.put("totalSchedules", examScheduleRepository.count());
        data.put("finalizedSchedules", examScheduleRepository.findAll().stream().filter(ExamSchedule::isFinalized).count());

        // 6. Library & Grievance Stats
        data.put("totalBooks", bookRepository.count());
        data.put("totalLoans", bookIssueRepository.count());
        data.put("unpaidFines", bookIssueRepository.calculateTotalUnpaidFines());
        data.put("totalGrievances", grievanceRepository.count());
        data.put("openGrievances", grievanceRepository.countByStatus("OPEN"));
        data.put("resolvedGrievances", grievanceRepository.countByStatus("RESOLVED"));

        return data;
    }

    public String generateCsvReport(Long departmentId, Long courseId, Integer semester) {
        Map<String, Object> data = getOverallAnalytics(departmentId, courseId, semester);
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value,Unit\n");
        csv.append("Total Enrolled Students,").append(data.get("totalStudents")).append(",Students\n");
        csv.append("Overall Attendance Rate,").append(data.get("overallAttendanceRate")).append(",%\n");
        csv.append("Low Attendance Defaulters (<75%),").append(data.get("lowAttendanceCount")).append(",Students\n");
        csv.append("Academic Pass Percentage,").append(data.get("passPercentage")).append(",%\n");
        csv.append("Academic Fail Percentage,").append(data.get("failPercentage")).append(",%\n");
        csv.append("Average Examination Score,").append(data.get("averageScore")).append(",Marks\n");
        csv.append("Total Fees Billed,").append(data.get("totalFeesBilled")).append(",USD\n");
        csv.append("Total Fees Collected,").append(data.get("collectedFees")).append(",USD\n");
        csv.append("Total Fees Pending,").append(data.get("pendingFees")).append(",USD\n");
        csv.append("Fee Recovery Rate,").append(data.get("feeCollectionRate")).append(",%\n");
        csv.append("Total Books in Library,").append(data.get("totalBooks")).append(",Volumes\n");
        csv.append("Active/Past Book Loans,").append(data.get("totalLoans")).append(",Issues\n");
        csv.append("Unpaid Library Fines,").append(data.get("unpaidFines")).append(",USD\n");
        csv.append("Total Grievance Tickets,").append(data.get("totalGrievances")).append(",Tickets\n");
        csv.append("Resolved Grievance Cases,").append(data.get("resolvedGrievances")).append(",Tickets\n");
        return csv.toString();
    }

    public byte[] generateAnalyticsPdfReport(Long departmentId, Long courseId, Integer semester) {
        Map<String, Object> data = getOverallAnalytics(departmentId, courseId, semester);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(30, 58, 138));
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(71, 85, 105));
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);

            Paragraph title = new Paragraph("METROPOLITAN UNIVERSITY OF TECHNOLOGY", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("EXECUTIVE INSTITUTIONAL ANALYTICS & AUDIT REPORT", fontSubtitle);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);

            Paragraph meta = new Paragraph("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), fontBody);
            meta.setAlignment(Element.ALIGN_RIGHT);
            meta.setSpacingAfter(10);
            document.add(meta);

            // Table of Key Performance Indicators
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{40f, 35f, 25f});

            addHeaderCell(table, "Institutional Domain", fontHeader);
            addHeaderCell(table, "Key Performance Indicator", fontHeader);
            addHeaderCell(table, "Current Status", fontHeader);

            addRow(table, "Student Enrollment", "Total Registered Students", String.valueOf(data.get("totalStudents")), fontBody, fontBold);
            addRow(table, "Student Attendance", "Average Attendance Rate", data.get("overallAttendanceRate") + "%", fontBody, fontBold);
            addRow(table, "Student Attendance", "Students Below 75% Threshold", String.valueOf(data.get("lowAttendanceCount")), fontBody, fontBold);
            addRow(table, "Academics & Exams", "Average Examination Score", data.get("averageScore") + "%", fontBody, fontBold);
            addRow(table, "Academics & Exams", "Passing Rate Across Subjects", data.get("passPercentage") + "%", fontBody, fontBold);
            addRow(table, "Finance & Accounts", "Total Fees Collected", "$" + data.get("collectedFees"), fontBody, fontBold);
            addRow(table, "Finance & Accounts", "Pending Fee Dues", "$" + data.get("pendingFees"), fontBody, fontBold);
            addRow(table, "Finance & Accounts", "Fee Recovery Rate", data.get("feeCollectionRate") + "%", fontBody, fontBold);
            addRow(table, "Library Resources", "Catalog Total Titles", String.valueOf(data.get("totalBooks")), fontBody, fontBold);
            addRow(table, "Library Resources", "Circulation Loans", String.valueOf(data.get("totalLoans")), fontBody, fontBold);
            addRow(table, "Campus Grievance", "Total Cases Logged", String.valueOf(data.get("totalGrievances")), fontBody, fontBold);
            addRow(table, "Campus Grievance", "Resolved Complaints", String.valueOf(data.get("resolvedGrievances")), fontBody, fontBold);

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating analytics PDF: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(30, 58, 138));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, String col1, String col2, String col3, Font bodyFont, Font boldFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(col1, bodyFont));
        c1.setPadding(5);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(col2, bodyFont));
        c2.setPadding(5);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(col3, boldFont));
        c3.setPadding(5);
        c3.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c3);
    }
}
