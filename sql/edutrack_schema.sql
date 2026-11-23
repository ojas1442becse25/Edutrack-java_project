-- ============================================================================
-- PROJECT: EduTrack - Academic & Placement Tracking System
-- MILESTONE: 5 (Java 8+ Console Application with MySQL & JDBC)
-- ARCHITECTURE: 3-Layer (Presentation -> Service -> DAO -> MySQL)
-- TARGET RDBMS: MySQL 8.0+
-- FILE: sql/edutrack_schema.sql
-- DESCRIPTION: Complete DDL & DML script containing 12 tables, primary keys,
--              foreign keys, unique constraints, check constraints, indexes,
--              and sample test records.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- DATABASE CREATION & SETUP
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS edutrack_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE edutrack_db;

-- Temporarily disable foreign key checks for clean drops
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS AUDIT_LOG;
DROP TABLE IF EXISTS APPLICATION;
DROP TABLE IF EXISTS PLACEMENT_DRIVE;
DROP TABLE IF EXISTS COMPANY;
DROP TABLE IF EXISTS QUIZ_SUBMISSION;
DROP TABLE IF EXISTS QUIZ;
DROP TABLE IF EXISTS CLASS_RESOURCE;
DROP TABLE IF EXISTS CLASS_PROGRESS_LOG;
DROP TABLE IF EXISTS CLASS_ENROLLMENT;
DROP TABLE IF EXISTS `CLASS`;
DROP TABLE IF EXISTS STUDENT;
DROP TABLE IF EXISTS TEACHER;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 1. TABLE: TEACHER
-- Description: Stores faculty details, department, and teaching status.
-- ============================================================================
CREATE TABLE TEACHER (
    teacher_id INT AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(50) NOT NULL,
    designation VARCHAR(50) NOT NULL DEFAULT 'Assistant Professor',
    hire_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_teacher PRIMARY KEY (teacher_id),
    CONSTRAINT uq_teacher_email UNIQUE (email),
    CONSTRAINT uq_teacher_phone UNIQUE (phone),
    CONSTRAINT chk_teacher_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ON_LEAVE'))
) ENGINE=InnoDB;

CREATE INDEX idx_teacher_dept ON TEACHER (department);
CREATE INDEX idx_teacher_status ON TEACHER (status);

-- ============================================================================
-- 2. TABLE: STUDENT
-- Description: Stores enrolled student profiles, academic performance (CGPA),
--              and semester standing.
-- ============================================================================
CREATE TABLE STUDENT (
    student_id INT AUTO_INCREMENT,
    roll_number VARCHAR(30) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(50) NOT NULL,
    semester INT NOT NULL,
    cgpa DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    enrollment_year INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_student PRIMARY KEY (student_id),
    CONSTRAINT uq_student_roll UNIQUE (roll_number),
    CONSTRAINT uq_student_email UNIQUE (email),
    CONSTRAINT uq_student_phone UNIQUE (phone),
    CONSTRAINT chk_student_semester CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_student_cgpa CHECK (cgpa >= 0.00 AND cgpa <= 10.00),
    CONSTRAINT chk_student_year CHECK (enrollment_year >= 2000),
    CONSTRAINT chk_student_status CHECK (status IN ('ACTIVE', 'GRADUATED', 'DROPPED', 'SUSPENDED'))
) ENGINE=InnoDB;

CREATE INDEX idx_student_dept ON STUDENT (department);
CREATE INDEX idx_student_semester ON STUDENT (semester);
CREATE INDEX idx_student_cgpa ON STUDENT (cgpa);
CREATE INDEX idx_student_status ON STUDENT (status);

-- ============================================================================
-- 3. TABLE: CLASS
-- Description: Represents academic courses/sections managed by teachers.
-- Note: 'CLASS' is a reserved keyword in SQL and Java, escaped with backticks.
-- ============================================================================
CREATE TABLE `CLASS` (
    class_id INT AUTO_INCREMENT,
    class_code VARCHAR(30) NOT NULL,
    course_name VARCHAR(100) NOT NULL,
    teacher_id INT NOT NULL,
    semester INT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    credits INT NOT NULL DEFAULT 3,
    max_capacity INT NOT NULL DEFAULT 60,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_class PRIMARY KEY (class_id),
    CONSTRAINT uq_class_code UNIQUE (class_code),
    CONSTRAINT fk_class_teacher FOREIGN KEY (teacher_id)
        REFERENCES TEACHER (teacher_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_class_semester CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_class_credits CHECK (credits BETWEEN 1 AND 10),
    CONSTRAINT chk_class_capacity CHECK (max_capacity > 0),
    CONSTRAINT chk_class_status CHECK (status IN ('SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE INDEX idx_class_teacher ON `CLASS` (teacher_id);
CREATE INDEX idx_class_status ON `CLASS` (status);
CREATE INDEX idx_class_academic_year ON `CLASS` (academic_year);

-- ============================================================================
-- 4. TABLE: CLASS_ENROLLMENT
-- Description: Junction table tracking student enrollment in specific classes,
--              attendance percentages, and assigned final grades.
-- ============================================================================
CREATE TABLE CLASS_ENROLLMENT (
    enrollment_id INT AUTO_INCREMENT,
    class_id INT NOT NULL,
    student_id INT NOT NULL,
    enrollment_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    attendance_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    grade VARCHAR(5) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_enrollment PRIMARY KEY (enrollment_id),
    CONSTRAINT fk_enrollment_class FOREIGN KEY (class_id)
        REFERENCES `CLASS` (class_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id)
        REFERENCES STUDENT (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uq_class_student UNIQUE (class_id, student_id),
    CONSTRAINT chk_enrollment_attendance CHECK (attendance_percentage >= 0.00 AND attendance_percentage <= 100.00),
    CONSTRAINT chk_enrollment_grade CHECK (grade IS NULL OR grade IN ('A+', 'A', 'B+', 'B', 'C+', 'C', 'D', 'F', 'I')),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED'))
) ENGINE=InnoDB;

CREATE INDEX idx_enrollment_class ON CLASS_ENROLLMENT (class_id);
CREATE INDEX idx_enrollment_student ON CLASS_ENROLLMENT (student_id);
CREATE INDEX idx_enrollment_status ON CLASS_ENROLLMENT (status);

-- ============================================================================
-- 5. TABLE: CLASS_PROGRESS_LOG
-- Description: Logs syllabus progression, daily topics covered, and teaching hours.
-- ============================================================================
CREATE TABLE CLASS_PROGRESS_LOG (
    log_id INT AUTO_INCREMENT,
    class_id INT NOT NULL,
    log_date DATE NOT NULL,
    topic_covered VARCHAR(200) NOT NULL,
    hours_spent DECIMAL(4,2) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_progress_log PRIMARY KEY (log_id),
    CONSTRAINT fk_progress_class FOREIGN KEY (class_id)
        REFERENCES `CLASS` (class_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_progress_hours CHECK (hours_spent > 0.00 AND hours_spent <= 12.00)
) ENGINE=InnoDB;

CREATE INDEX idx_progress_class ON CLASS_PROGRESS_LOG (class_id);
CREATE INDEX idx_progress_date ON CLASS_PROGRESS_LOG (log_date);

-- ============================================================================
-- 6. TABLE: CLASS_RESOURCE
-- Description: Course materials, syllabus PDFs, lab manuals, and video links.
-- ============================================================================
CREATE TABLE CLASS_RESOURCE (
    resource_id INT AUTO_INCREMENT,
    class_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_url VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_resource PRIMARY KEY (resource_id),
    CONSTRAINT fk_resource_class FOREIGN KEY (class_id)
        REFERENCES `CLASS` (class_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_resource_type CHECK (resource_type IN ('SYLLABUS', 'NOTES', 'ASSIGNMENT', 'VIDEO', 'SLIDES', 'OTHER'))
) ENGINE=InnoDB;

CREATE INDEX idx_resource_class ON CLASS_RESOURCE (class_id);
CREATE INDEX idx_resource_type ON CLASS_RESOURCE (resource_type);

-- ============================================================================
-- 7. TABLE: QUIZ
-- Description: Internal assessments and quizzes created for a class.
-- ============================================================================
CREATE TABLE QUIZ (
    quiz_id INT AUTO_INCREMENT,
    class_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    total_marks INT NOT NULL,
    passing_marks INT NOT NULL,
    due_date DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_quiz PRIMARY KEY (quiz_id),
    CONSTRAINT fk_quiz_class FOREIGN KEY (class_id)
        REFERENCES `CLASS` (class_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_quiz_total_marks CHECK (total_marks > 0),
    CONSTRAINT chk_quiz_passing_marks CHECK (passing_marks > 0 AND passing_marks <= total_marks),
    CONSTRAINT chk_quiz_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED'))
) ENGINE=InnoDB;

CREATE INDEX idx_quiz_class ON QUIZ (class_id);
CREATE INDEX idx_quiz_status ON QUIZ (status);
CREATE INDEX idx_quiz_due_date ON QUIZ (due_date);

-- ============================================================================
-- 8. TABLE: QUIZ_SUBMISSION
-- Description: Records student performance and marks scored in quizzes.
-- ============================================================================
CREATE TABLE QUIZ_SUBMISSION (
    submission_id INT AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    student_id INT NOT NULL,
    score_obtained DECIMAL(5,2) NOT NULL,
    submission_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'EVALUATED',
    remarks VARCHAR(255),

    CONSTRAINT pk_submission PRIMARY KEY (submission_id),
    CONSTRAINT fk_submission_quiz FOREIGN KEY (quiz_id)
        REFERENCES QUIZ (quiz_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id)
        REFERENCES STUDENT (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uq_quiz_student UNIQUE (quiz_id, student_id),
    CONSTRAINT chk_submission_score CHECK (score_obtained >= 0.00),
    CONSTRAINT chk_submission_status CHECK (status IN ('SUBMITTED', 'EVALUATED', 'REJECTED'))
) ENGINE=InnoDB;

CREATE INDEX idx_submission_quiz ON QUIZ_SUBMISSION (quiz_id);
CREATE INDEX idx_submission_student ON QUIZ_SUBMISSION (student_id);

-- ============================================================================
-- 9. TABLE: COMPANY
-- Description: Recruiting partners participating in campus placement drives.
-- ============================================================================
CREATE TABLE COMPANY (
    company_id INT AUTO_INCREMENT,
    company_name VARCHAR(100) NOT NULL,
    industry VARCHAR(50) NOT NULL,
    website VARCHAR(150),
    contact_person VARCHAR(100) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(20),
    address VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_company PRIMARY KEY (company_id),
    CONSTRAINT uq_company_name UNIQUE (company_name),
    CONSTRAINT uq_company_email UNIQUE (contact_email),
    CONSTRAINT chk_company_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLACKLISTED'))
) ENGINE=InnoDB;

CREATE INDEX idx_company_industry ON COMPANY (industry);
CREATE INDEX idx_company_status ON COMPANY (status);

-- ============================================================================
-- 10. TABLE: PLACEMENT_DRIVE
-- Description: Job openings, packages (LPA), criteria, and interview dates.
-- ============================================================================
CREATE TABLE PLACEMENT_DRIVE (
    drive_id INT AUTO_INCREMENT,
    company_id INT NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    job_description TEXT,
    package_lpa DECIMAL(5,2) NOT NULL,
    min_cgpa_criteria DECIMAL(4,2) NOT NULL DEFAULT 6.00,
    eligible_department VARCHAR(100) NOT NULL DEFAULT 'ALL',
    drive_date DATE NOT NULL,
    last_date_to_apply DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_drive PRIMARY KEY (drive_id),
    CONSTRAINT fk_drive_company FOREIGN KEY (company_id)
        REFERENCES COMPANY (company_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_drive_package CHECK (package_lpa > 0.00),
    CONSTRAINT chk_drive_min_cgpa CHECK (min_cgpa_criteria >= 0.00 AND min_cgpa_criteria <= 10.00),
    CONSTRAINT chk_drive_status CHECK (status IN ('UPCOMING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE INDEX idx_drive_company ON PLACEMENT_DRIVE (company_id);
CREATE INDEX idx_drive_status ON PLACEMENT_DRIVE (status);
CREATE INDEX idx_drive_date ON PLACEMENT_DRIVE (drive_date);

-- ============================================================================
-- 11. TABLE: APPLICATION
-- Description: Tracks student job applications, interview progression & offers.
-- ============================================================================
CREATE TABLE APPLICATION (
    application_id INT AUTO_INCREMENT,
    drive_id INT NOT NULL,
    student_id INT NOT NULL,
    application_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'APPLIED',
    interview_feedback TEXT,

    CONSTRAINT pk_application PRIMARY KEY (application_id),
    CONSTRAINT fk_app_drive FOREIGN KEY (drive_id)
        REFERENCES PLACEMENT_DRIVE (drive_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_app_student FOREIGN KEY (student_id)
        REFERENCES STUDENT (student_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uq_drive_student UNIQUE (drive_id, student_id),
    CONSTRAINT chk_app_status CHECK (status IN (
        'APPLIED', 'SHORTLISTED', 'TECHNICAL_ROUND', 'HR_ROUND', 'OFFERED', 'REJECTED', 'WITHDRAWN'
    ))
) ENGINE=InnoDB;

CREATE INDEX idx_app_drive ON APPLICATION (drive_id);
CREATE INDEX idx_app_student ON APPLICATION (student_id);
CREATE INDEX idx_app_status ON APPLICATION (status);

-- ============================================================================
-- 12. TABLE: AUDIT_LOG
-- Description: System-wide audit trail recording critical actions (INSERT,
--              UPDATE, DELETE, AUTH) for security and accountability.
-- ============================================================================
CREATE TABLE AUDIT_LOG (
    log_id INT AUTO_INCREMENT,
    entity_name VARCHAR(50) NOT NULL,
    entity_id INT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT,

    CONSTRAINT pk_audit_log PRIMARY KEY (log_id),
    CONSTRAINT chk_audit_action CHECK (action_type IN ('INSERT', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT'))
) ENGINE=InnoDB;

CREATE INDEX idx_audit_entity ON AUDIT_LOG (entity_name, entity_id);
CREATE INDEX idx_audit_action ON AUDIT_LOG (action_type);
CREATE INDEX idx_audit_timestamp ON AUDIT_LOG (action_timestamp);


-- ============================================================================
-- SAMPLE TEST DATA INSERTIONS (2-3 Records Per Table)
-- ============================================================================

-- 1. Insert TEACHER records
INSERT INTO TEACHER (teacher_id, first_name, last_name, email, phone, department, designation, hire_date, status) VALUES
(1, 'Dr. Alan', 'Turing', 'alan.turing@edutrack.edu', '+91-9876543210', 'Computer Science', 'Professor', '2018-07-01', 'ACTIVE'),
(2, 'Dr. Grace', 'Hopper', 'grace.hopper@edutrack.edu', '+91-9876543211', 'Computer Science', 'Associate Professor', '2019-08-15', 'ACTIVE'),
(3, 'Prof. Claude', 'Shannon', 'claude.shannon@edutrack.edu', '+91-9876543212', 'Information Technology', 'Assistant Professor', '2021-01-10', 'ACTIVE');

-- 2. Insert STUDENT records
INSERT INTO STUDENT (student_id, roll_number, first_name, last_name, email, phone, department, semester, cgpa, enrollment_year, status) VALUES
(1, 'CS2023001', 'Rahul', 'Sharma', 'rahul.sharma@student.edu', '+91-9123456780', 'Computer Science', 6, 8.75, 2023, 'ACTIVE'),
(2, 'CS2023002', 'Priya', 'Nair', 'priya.nair@student.edu', '+91-9123456781', 'Computer Science', 6, 9.20, 2023, 'ACTIVE'),
(3, 'IT2023003', 'Aman', 'Verma', 'aman.verma@student.edu', '+91-9123456782', 'Information Technology', 6, 7.40, 2023, 'ACTIVE');

-- 3. Insert CLASS records
INSERT INTO `CLASS` (class_id, class_code, course_name, teacher_id, semester, academic_year, credits, max_capacity, status) VALUES
(1, 'CS301', 'Advanced Database Management Systems', 1, 6, '2025-2026', 4, 60, 'ONGOING'),
(2, 'CS302', 'Object Oriented Programming with Java', 2, 6, '2025-2026', 4, 60, 'ONGOING'),
(3, 'IT305', 'Computer Networks & Security', 3, 6, '2025-2026', 3, 50, 'ONGOING');

-- 4. Insert CLASS_ENROLLMENT records
INSERT INTO CLASS_ENROLLMENT (enrollment_id, class_id, student_id, enrollment_date, attendance_percentage, grade, status) VALUES
(1, 1, 1, '2026-01-05', 92.50, 'A', 'ENROLLED'),
(2, 1, 2, '2026-01-05', 98.00, 'A+', 'ENROLLED'),
(3, 2, 3, '2026-01-06', 81.00, 'B+', 'ENROLLED');

-- 5. Insert CLASS_PROGRESS_LOG records
INSERT INTO CLASS_PROGRESS_LOG (log_id, class_id, log_date, topic_covered, hours_spent, remarks) VALUES
(1, 1, '2026-02-02', 'Relational Normalization: 1NF to BCNF', 2.00, 'All examples discussed and solved in class.'),
(2, 1, '2026-02-04', 'Indexing Techniques: B+ Trees and Hashing', 2.00, 'Conducted interactive query execution plan demo.'),
(3, 2, '2026-02-03', 'JDBC Architecture & 3-Layer Design Pattern', 2.50, 'Demonstrated Connection, Statement, and PreparedStatement.');

-- 6. Insert CLASS_RESOURCE records
INSERT INTO CLASS_RESOURCE (resource_id, class_id, title, resource_type, resource_url) VALUES
(1, 1, 'DBMS Complete Syllabus & Lab Guidelines', 'SYLLABUS', 'https://edutrack.edu/resources/cs301_syllabus.pdf'),
(2, 1, 'Lecture Notes - Transaction ACID Properties', 'NOTES', 'https://edutrack.edu/resources/cs301_acid_notes.pdf'),
(3, 2, 'Java JDBC Best Practices Presentation', 'SLIDES', 'https://edutrack.edu/resources/cs302_jdbc_slides.pptx');

-- 7. Insert QUIZ records
INSERT INTO QUIZ (quiz_id, class_id, title, total_marks, passing_marks, due_date, status) VALUES
(1, 1, 'Quiz 1: SQL Joins and Aggregations', 25, 10, '2026-02-28 23:59:59', 'PUBLISHED'),
(2, 1, 'Quiz 2: Transaction Isolation Levels', 20, 8, '2026-03-15 23:59:59', 'PUBLISHED'),
(3, 2, 'Quiz 1: Java Collections & Exception Handling', 30, 12, '2026-03-01 23:59:59', 'PUBLISHED');

-- 8. Insert QUIZ_SUBMISSION records
INSERT INTO QUIZ_SUBMISSION (submission_id, quiz_id, student_id, score_obtained, submission_date, status, remarks) VALUES
(1, 1, 1, 23.50, '2026-02-25 14:30:00', 'EVALUATED', 'Excellent performance on complex JOIN queries.'),
(2, 1, 2, 25.00, '2026-02-24 16:45:00', 'EVALUATED', 'Perfect score! Outstanding analytical understanding.'),
(3, 3, 3, 22.00, '2026-02-27 10:15:00', 'EVALUATED', 'Good effort; review custom exception chaining.');

-- 9. Insert COMPANY records
INSERT INTO COMPANY (company_id, company_name, industry, website, contact_person, contact_email, contact_phone, address, status) VALUES
(1, 'TechNova Solutions Ltd.', 'Information Technology', 'https://technova.example.com', 'Rajesh Kulkarni', 'hr@technova.example.com', '+91-22-25678901', 'Mindspace Tech Park, Mumbai, MH', 'ACTIVE'),
(2, 'CloudScale Systems', 'Cloud Infrastructure', 'https://cloudscale.example.com', 'Anita Desai', 'careers@cloudscale.example.com', '+91-80-45678902', 'Whitefield IT Hub, Bengaluru, KA', 'ACTIVE'),
(3, 'FinPulse Analytics', 'FinTech', 'https://finpulse.example.com', 'Vikram Malhotra', 'talent@finpulse.example.com', '+91-124-3456789', 'Cyber City, Gurugram, HR', 'ACTIVE');

-- 10. Insert PLACEMENT_DRIVE records
INSERT INTO PLACEMENT_DRIVE (drive_id, company_id, job_title, job_description, package_lpa, min_cgpa_criteria, eligible_department, drive_date, last_date_to_apply, status) VALUES
(1, 1, 'Associate Software Engineer - Java Backend', 'Development of scalable REST microservices with Spring Boot and MySQL.', 8.50, 7.50, 'Computer Science, Information Technology', '2026-03-20', '2026-03-10', 'UPCOMING'),
(2, 2, 'Cloud Infrastructure Engineer', 'Deployment automation, Kubernetes orchestration, and AWS/GCP operations.', 10.00, 8.00, 'Computer Science, Information Technology', '2026-03-25', '2026-03-15', 'UPCOMING'),
(3, 3, 'Data Analyst / FinTech Engineer', 'Financial data engineering, ETL pipelines, and SQL performance tuning.', 7.50, 7.00, 'ALL', '2026-04-05', '2026-03-25', 'UPCOMING');

-- 11. Insert APPLICATION records
INSERT INTO APPLICATION (application_id, drive_id, student_id, application_date, status, interview_feedback) VALUES
(1, 1, 1, '2026-02-15 11:20:00', 'SHORTLISTED', 'Cleared initial online aptitude test with 92% percentile.'),
(2, 1, 2, '2026-02-15 11:45:00', 'TECHNICAL_ROUND', 'Profile shortlisted; scheduled for Technical Round 1.'),
(3, 3, 3, '2026-02-16 09:30:00', 'APPLIED', 'Application submitted; awaiting CGPA screening.');

-- 12. Insert AUDIT_LOG records
INSERT INTO AUDIT_LOG (log_id, entity_name, entity_id, action_type, performed_by, action_timestamp, details) VALUES
(1, 'SYSTEM', 0, 'INSERT', 'SETUP_SCRIPT', '2026-11-23 12:00:00', 'Initial database schema and seed data deployed successfully.'),
(2, 'STUDENT', 1, 'INSERT', 'admin@edutrack.edu', '2026-11-23 12:05:00', 'Student Rahul Sharma registered under CS department.'),
(3, 'PLACEMENT_DRIVE', 1, 'INSERT', 'tpo@edutrack.edu', '2026-11-23 12:10:00', 'Placement drive created for TechNova Solutions Ltd (8.5 LPA).');

-- Ensure all changes are committed
COMMIT;

-- Verification summary output
SELECT 'TEACHER' AS Table_Name, COUNT(*) AS Total_Records FROM TEACHER
UNION ALL SELECT 'STUDENT', COUNT(*) FROM STUDENT
UNION ALL SELECT 'CLASS', COUNT(*) FROM `CLASS`
UNION ALL SELECT 'CLASS_ENROLLMENT', COUNT(*) FROM CLASS_ENROLLMENT
UNION ALL SELECT 'CLASS_PROGRESS_LOG', COUNT(*) FROM CLASS_PROGRESS_LOG
UNION ALL SELECT 'CLASS_RESOURCE', COUNT(*) FROM CLASS_RESOURCE
UNION ALL SELECT 'QUIZ', COUNT(*) FROM QUIZ
UNION ALL SELECT 'QUIZ_SUBMISSION', COUNT(*) FROM QUIZ_SUBMISSION
UNION ALL SELECT 'COMPANY', COUNT(*) FROM COMPANY
UNION ALL SELECT 'PLACEMENT_DRIVE', COUNT(*) FROM PLACEMENT_DRIVE
UNION ALL SELECT 'APPLICATION', COUNT(*) FROM APPLICATION
UNION ALL SELECT 'AUDIT_LOG', COUNT(*) FROM AUDIT_LOG;
