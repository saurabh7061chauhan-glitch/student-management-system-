package com.example.studentmanagement.service;

import com.example.studentmanagement.dto.MarksDTO;
import com.example.studentmanagement.entity.Marks;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.entity.Subject;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.MarksRepository;
import com.example.studentmanagement.repository.StudentRepository;
import com.example.studentmanagement.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MarksService {

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    public List<Marks> getAllMarks() {
        return marksRepository.findAll();
    }

    public List<Marks> getMarksByStudent(Long studentId) {
        return marksRepository.findByStudentIdOrderBySemesterAsc(studentId);
    }

    public List<Marks> getMarksByStudentAndSemester(Long studentId, int semester) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        return marksRepository.findByStudentAndSemester(student, semester);
    }

    public Marks getMarksById(Long id) {
        return marksRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marks record not found: " + id));
    }

    @Transactional
    public Marks saveOrUpdateMarks(MarksDTO dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + dto.getStudentId()));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + dto.getSubjectId()));

        Optional<Marks> existing = marksRepository.findByStudentAndSubjectAndSemester(
                student, subject, dto.getSemester());

        Marks marks;
        if (dto.getId() != null) {
            marks = getMarksById(dto.getId());
        } else if (existing.isPresent()) {
            marks = existing.get();
        } else {
            marks = new Marks();
            marks.setStudent(student);
            marks.setSubject(subject);
            marks.setSemester(dto.getSemester());
        }

        marks.setInternalMarks(dto.getInternalMarks());
        marks.setAssignmentMarks(dto.getAssignmentMarks());
        marks.setPracticalMarks(dto.getPracticalMarks());
        double examM = dto.getExamMarks() > 0 ? dto.getExamMarks() : dto.getExternalMarks();
        marks.setExamMarks(examM);
        marks.setExternalMarks(examM);
        marks.setMaxMarks(dto.getMaxMarks() > 0 ? dto.getMaxMarks() : 100.0);
        marks.calculateCalculatedFields();

        return marksRepository.save(marks);
    }

    @Transactional
    public Marks saveOrUpdateMarksForFaculty(MarksDTO dto, com.example.studentmanagement.entity.Teacher teacher) {
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + dto.getSubjectId()));
        if (subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Forbidden: You are only allowed to manage marks for your assigned subjects.");
        }
        return saveOrUpdateMarks(dto);
    }

    public List<Marks> getMarksForFaculty(com.example.studentmanagement.entity.Teacher teacher) {
        List<Subject> subjects = subjectRepository.findByTeacher(teacher);
        if (subjects.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return marksRepository.findBySubjectIn(subjects);
    }

    public List<Marks> getMarksForFacultyBySubject(com.example.studentmanagement.entity.Teacher teacher, Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        if (subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Forbidden: Not your assigned subject.");
        }
        return marksRepository.findBySubject(subject);
    }

    @Transactional
    public void deleteMarks(Long id) {
        if (!marksRepository.existsById(id)) {
            throw new ResourceNotFoundException("Marks record not found: " + id);
        }
        marksRepository.deleteById(id);
    }

    public static class StudentResultSummary {
        private double totalMarksObtained;
        private double totalMaxMarks;
        private double overallPercentage;
        private String overallGrade;
        private String overallStatus;
        private int totalSubjects;
        private int passedSubjects;

        public StudentResultSummary(double obtained, double max, double pct, String grade, String status, int total, int passed) {
            this.totalMarksObtained = obtained;
            this.totalMaxMarks = max;
            this.overallPercentage = pct;
            this.overallGrade = grade;
            this.overallStatus = status;
            this.totalSubjects = total;
            this.passedSubjects = passed;
        }

        public double getTotalMarksObtained() { return totalMarksObtained; }
        public double getTotalMaxMarks() { return totalMaxMarks; }
        public double getOverallPercentage() { return overallPercentage; }
        public String getOverallGrade() { return overallGrade; }
        public String getOverallStatus() { return overallStatus; }
        public int getTotalSubjects() { return totalSubjects; }
        public int getPassedSubjects() { return passedSubjects; }

        public double getGpa() {
            return Math.round((overallPercentage / 10.0) * 10.0) / 10.0;
        }
        public double getCgpa() { return getGpa(); }
        public double getSgpa() { return getGpa(); }
        public double getPercentage() { return overallPercentage; }
        public String getGrade() { return overallGrade; }
        public String getFinalGrade() { return overallGrade; }
        public String getStatus() { return overallStatus; }
        public double getTotalMarks() { return totalMarksObtained; }
        public double getMaxPossibleMarks() { return totalMaxMarks; }
    }

    public StudentResultSummary calculateStudentSummary(Long studentId) {
        List<Marks> marksList = getMarksByStudent(studentId);
        if (marksList.isEmpty()) {
            return new StudentResultSummary(0, 0, 0, "N/A", "No Records", 0, 0);
        }

        double totalObtained = 0;
        double totalMax = 0;
        int passed = 0;

        for (Marks m : marksList) {
            totalObtained += m.getTotalMarks();
            totalMax += m.getMaxMarks();
            if ("Pass".equalsIgnoreCase(m.getStatus())) {
                passed++;
            }
        }

        double pct = totalMax > 0 ? Math.round((totalObtained / totalMax * 100.0) * 10.0) / 10.0 : 0.0;
        String grade = pct >= 90 ? "A+" : pct >= 80 ? "A" : pct >= 70 ? "B+" : pct >= 60 ? "B" : pct >= 50 ? "C" : pct >= 40 ? "D" : "F";
        String status = (passed == marksList.size() && pct >= 40.0) ? "Passed" : "Needs Improvement / Compartment";

        return new StudentResultSummary(totalObtained, totalMax, pct, grade, status, marksList.size(), passed);
    }

    public List<Integer> getDistinctSemestersByStudent(Long studentId) {
        List<Marks> allMarks = getMarksByStudent(studentId);
        return allMarks.stream()
                .map(Marks::getSemester)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    public StudentResultSummary calculateStudentSummaryForSemester(Long studentId, int semester) {
        List<Marks> marksList = getMarksByStudentAndSemester(studentId, semester);
        if (marksList.isEmpty()) {
            return new StudentResultSummary(0, 0, 0, "N/A", "No Records", 0, 0);
        }

        double totalObtained = 0;
        double totalMax = 0;
        int passed = 0;

        for (Marks m : marksList) {
            totalObtained += m.getTotalMarks();
            totalMax += m.getMaxMarks();
            if ("Pass".equalsIgnoreCase(m.getStatus())) {
                passed++;
            }
        }

        double pct = totalMax > 0 ? Math.round((totalObtained / totalMax * 100.0) * 10.0) / 10.0 : 0.0;
        String grade = pct >= 90 ? "A+" : pct >= 80 ? "A" : pct >= 70 ? "B+" : pct >= 60 ? "B" : pct >= 50 ? "C" : pct >= 40 ? "D" : "F";
        String status = (passed == marksList.size() && pct >= 40.0) ? "Passed" : "Needs Improvement / Compartment";

        return new StudentResultSummary(totalObtained, totalMax, pct, grade, status, marksList.size(), passed);
    }

    public static class SemesterPerformanceRecord {
        private int semester;
        private double totalMarksObtained;
        private double totalMaxMarks;
        private double percentage;
        private double sgpa;
        private String grade;
        private String status;
        private int totalSubjects;
        private int passedSubjects;
        private int totalCredits;
        private List<Marks> marks;

        public SemesterPerformanceRecord(int semester, double totalObtained, double totalMax, double percentage,
                                         double sgpa, String grade, String status, int totalSubjects, int passedSubjects,
                                         int totalCredits, List<Marks> marks) {
            this.semester = semester;
            this.totalMarksObtained = totalObtained;
            this.totalMaxMarks = totalMax;
            this.percentage = percentage;
            this.sgpa = sgpa;
            this.grade = grade;
            this.status = status;
            this.totalSubjects = totalSubjects;
            this.passedSubjects = passedSubjects;
            this.totalCredits = totalCredits;
            this.marks = marks;
        }

        public int getSemester() { return semester; }
        public double getTotalMarksObtained() { return totalMarksObtained; }
        public double getTotalMaxMarks() { return totalMaxMarks; }
        public double getPercentage() { return percentage; }
        public double getSgpa() { return sgpa; }
        public String getGrade() { return grade; }
        public String getStatus() { return status; }
        public int getTotalSubjects() { return totalSubjects; }
        public int getPassedSubjects() { return passedSubjects; }
        public int getTotalCredits() { return totalCredits; }
        public List<Marks> getMarks() { return marks; }
    }

    public List<SemesterPerformanceRecord> getSemesterPerformanceHistory(Long studentId) {
        List<Integer> semesters = getDistinctSemestersByStudent(studentId);
        List<SemesterPerformanceRecord> history = new java.util.ArrayList<>();

        for (Integer sem : semesters) {
            List<Marks> semMarks = getMarksByStudentAndSemester(studentId, sem);
            double totalObtained = 0;
            double totalMax = 0;
            int passed = 0;
            int credits = 0;

            for (Marks m : semMarks) {
                totalObtained += m.getTotalMarks();
                totalMax += m.getMaxMarks();
                if ("Pass".equalsIgnoreCase(m.getStatus())) {
                    passed++;
                }
                if (m.getSubject() != null) {
                    credits += m.getSubject().getCredits();
                }
            }

            double pct = totalMax > 0 ? Math.round((totalObtained / totalMax * 100.0) * 10.0) / 10.0 : 0.0;
            double sgpa = calculateSgpa(semMarks);
            String grade = pct >= 90 ? "A+" : pct >= 80 ? "A" : pct >= 70 ? "B+" : pct >= 60 ? "B" : pct >= 50 ? "C" : pct >= 40 ? "D" : "F";
            String status = (passed == semMarks.size() && pct >= 40.0) ? "Passed" : "Needs Improvement";

            history.add(new SemesterPerformanceRecord(
                    sem, totalObtained, totalMax, pct, sgpa, grade, status, semMarks.size(), passed, credits, semMarks
            ));
        }

        return history;
    }

    public double calculateSgpa(List<Marks> marksList) {
        if (marksList == null || marksList.isEmpty()) return 0.0;
        double weightedPoints = 0.0;
        int totalCredits = 0;
        for (Marks m : marksList) {
            int credits = (m.getSubject() != null && m.getSubject().getCredits() > 0) ? m.getSubject().getCredits() : 3;
            double pts = getGradePoints(m.getPercentage());
            weightedPoints += (credits * pts);
            totalCredits += credits;
        }
        return totalCredits > 0 ? Math.round((weightedPoints / totalCredits) * 100.0) / 100.0 : 0.0;
    }

    public double calculateCgpa(Long studentId) {
        List<Marks> allMarks = marksRepository.findByStudentIdOrderBySemesterAsc(studentId);
        if (allMarks.isEmpty()) return 0.0;
        return calculateSgpa(allMarks);
    }

    public double getGradePoints(double percentage) {
        if (percentage >= 90.0) return 10.0;
        if (percentage >= 80.0) return 9.0;
        if (percentage >= 70.0) return 8.0;
        if (percentage >= 60.0) return 7.0;
        if (percentage >= 50.0) return 6.0;
        if (percentage >= 40.0) return 5.0;
        return 0.0;
    }
}
