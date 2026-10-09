package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.Notice;
import com.example.studentmanagement.entity.NoticeReadStatus;
import com.example.studentmanagement.entity.Student;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import com.example.studentmanagement.repository.NoticeReadStatusRepository;
import com.example.studentmanagement.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeReadStatusRepository readStatusRepository;

    @Autowired
    private com.example.studentmanagement.repository.DepartmentRepository departmentRepository;

    @Autowired
    private com.example.studentmanagement.repository.CourseRepository courseRepository;

    @Autowired
    private com.example.studentmanagement.repository.SectionRepository sectionRepository;

    @Autowired
    private com.example.studentmanagement.repository.StudentRepository studentRepository;

    public List<Notice> getAllNotices() {
        return noticeRepository.findAllByOrderByPostedDateDesc();
    }

    public List<Notice> getNoticesForStudent() {
        return noticeRepository.findByTargetAudienceInOrderByPostedDateDesc(Arrays.asList("ALL", "STUDENT"));
    }

    public List<Notice> getNoticesForStudent(Student student) {
        if (student == null) {
            return getNoticesForStudent();
        }
        List<Notice> all = getAllNotices();
        return all.stream().filter(n -> {
            String target = n.getTargetAudience() != null ? n.getTargetAudience().toUpperCase() : "ALL";
            if ("ALL".equals(target) || "STUDENT".equals(target)) return true;
            if ("DEPARTMENT".equals(target) && n.getDepartment() != null && student.getDepartment() != null) {
                return n.getDepartment().getId().equals(student.getDepartment().getId());
            }
            if ("COURSE".equals(target) && n.getCourse() != null && student.getCourse() != null) {
                return n.getCourse().getId().equals(student.getCourse().getId());
            }
            if ("SEMESTER".equals(target) && n.getSemester() != null) {
                boolean semMatch = n.getSemester().equals(student.getSemester());
                boolean courseMatch = n.getCourse() == null || (student.getCourse() != null && n.getCourse().getId().equals(student.getCourse().getId()));
                return semMatch && courseMatch;
            }
            if ("SECTION".equals(target) && n.getSection() != null && student.getSection() != null) {
                return n.getSection().getId().equals(student.getSection().getId());
            }
            if ("INDIVIDUAL".equals(target) && n.getStudent() != null) {
                return n.getStudent().getId().equals(student.getId());
            }
            return false;
        }).collect(Collectors.toList());
    }

    public boolean isNoticeRead(Notice notice, Student student) {
        if (student == null) return false;
        return readStatusRepository.existsByNoticeAndStudent(notice, student);
    }

    @Transactional
    public void markNoticeAsRead(Long noticeId, Student student) {
        if (student == null) return;
        Notice notice = getNoticeById(noticeId);
        if (!readStatusRepository.existsByNoticeAndStudent(notice, student)) {
            readStatusRepository.save(new NoticeReadStatus(notice, student));
        }
    }

    public void markAsRead(Student student, Long noticeId) {
        markNoticeAsRead(noticeId, student);
    }

    public long getUnreadNoticeCount(Student student) {
        if (student == null) return 0;
        List<Notice> forStudent = getNoticesForStudent(student);
        long readCount = forStudent.stream().filter(n -> readStatusRepository.existsByNoticeAndStudent(n, student)).count();
        return Math.max(0, forStudent.size() - readCount);
    }

    public long getUnreadCount(Student student) {
        return getUnreadNoticeCount(student);
    }

    public List<Notice> getNoticesForFaculty() {
        return noticeRepository.findByTargetAudienceInOrderByPostedDateDesc(Arrays.asList("ALL", "TEACHER", "FACULTY"));
    }

    public List<Notice> getRecentNotices() {
        return noticeRepository.findTop5ByOrderByPostedDateDesc();
    }

    public Notice getNoticeById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found: " + id));
    }

    @Transactional
    public Notice saveNotice(Notice notice) {
        if (notice.getPostedDate() == null) {
            notice.setPostedDate(LocalDate.now());
        }
        if (notice.getDepartment() != null && notice.getDepartment().getId() != null) {
            notice.setDepartment(departmentRepository.findById(notice.getDepartment().getId()).orElse(null));
        }
        if (notice.getCourse() != null && notice.getCourse().getId() != null) {
            notice.setCourse(courseRepository.findById(notice.getCourse().getId()).orElse(null));
        }
        if (notice.getSection() != null && notice.getSection().getId() != null) {
            notice.setSection(sectionRepository.findById(notice.getSection().getId()).orElse(null));
        }
        if (notice.getStudent() != null && notice.getStudent().getId() != null) {
            notice.setStudent(studentRepository.findById(notice.getStudent().getId()).orElse(null));
        }
        return noticeRepository.save(notice);
    }

    @Transactional
    public void deleteNotice(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notice not found: " + id);
        }
        noticeRepository.deleteById(id);
    }
}
