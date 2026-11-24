# EduTrack — Academic & Campus Placement Management System

**EduTrack** is an enterprise-grade Milestone 5 Java + MySQL Console Application designed for universities, engineering colleges, and higher education institutes. It delivers an end-to-end platform bridging faculty course management, student academic self-service, internal assessments, and campus placement recruiting drives with thread-safe slot allocation.

---

## 🌟 Key Features by User Role

### 1. 🎓 Faculty & Instructor Portal
* **Course Sections Management**: Create and configure semester courses with credit ratings and capacity ceilings.
* **Teaching Progress Diary**: Log daily lectures, syllabus topic progression, and teaching hours.
* **Course Resource Repository**: Upload syllabus documents, lecture notes, video links, and presentations.
* **Assessment & Quiz Authoring**: Design quizzes with customizable passing cutoff marks and submission deadlines.
* **Grading & Evaluation Feedback**: Review student quiz submissions and assign qualitative feedback notes.
* **Early-Warning Engine**: Detect at-risk students flagged automatically when course attendance drops below 60% or when a failing grade (`F`) is assigned.

### 2. 👨‍🎓 Student Self-Service Portal
* **Academic Dashboard**: Instant view of enrolled courses, credits, overall attendance percentage, and quiz grade averages.
* **Study Material Access**: Browse and retrieve lecture notes, slides, and syllabus documents by course.
* **Online Quiz Engine**: Attempt active course quizzes with deadline validation and automatic pass/fail evaluation.
* **Campus Placement Hub**: Explore active recruitment drives, packages (LPA), eligibility cutoff criteria, and application deadlines.
* **Thread-Safe Application Submission**: Apply for placement drives with automated validation against disciplinary standing, department eligibility, and CGPA cutoffs.
* **Application Tracker**: Follow real-time interview progression from `APPLIED` to `SHORTLISTED`, `TECHNICAL_ROUND`, `HR_ROUND`, and `OFFERED`.

### 3. 🏢 University & Placement Administration Portal
* **Corporate Partner Onboarding**: Register recruiters, industry sector details, and HR contact coordinates.
* **Placement Drive Activation**: Create drives with specific CGPA criteria, annual package (LPA), and vacancy caps.
* **Candidate Review Pipeline**: Filter applicant pools and advance qualified candidates through interview stages.
* **System Audit Trail**: Access the immutable `AUDIT_LOG` recording sensitive enrollment and job application events.
* **Student & Faculty Provisioning**: Onboard new students and instructors with automated duplicate key prevention.

---

## 🛠 Technology Stack

* **Programming Language**: Core Java (Java 8+ / JDK 17 / JDK 21 / JDK 27 compatible)
* **Relational Database**: MySQL Server 8.0+ (InnoDB Storage Engine with strict foreign key constraints)
* **Database Driver**: MySQL Connector/J `8.0.33` (`mysql-connector-java-8.0.33.jar`)
* **Persistence Mechanism**: Standard JDBC (`java.sql.*`) with `DriverManager`, `PreparedStatement`, and `ResultSet`
* **Concurrency Model**: Multithreaded synchronization utilizing per-drive monitor locks and atomic transaction boundaries
* **Architecture**: Strict 3-Layer Enterprise Design Pattern (Presentation $\to$ Service $\to$ DAO $\to$ MySQL)

---

## 📂 Project Directory Structure

```
d:\Edutrack\
├── bin/                             # Compiled Java bytecode (.class files)
├── docs/                            # Complete system documentation
│   ├── ARCHITECTURE.md              # 3-Layer design & viva concepts guide
│   ├── README.md                    # Project overview & feature guide
│   └── SETUP.md                     # Step-by-step setup & execution instructions
├── evidence/                        # Automated verification reports
│   ├── step1_verification.txt       # Schema & connection verification
│   ├── step2_verification.txt       # Core DAOs CRUD verification
│   ├── step3_verification.txt       # Atomic transactions (Commit & Rollback)
│   ├── step4_verification.txt       # Service layer & concurrency verification
│   └── step5_verification.txt       # Full End-to-End workflow integration test
├── lib/                             # External runtime libraries
│   └── mysql-connector-java-8.0.33.jar
├── sql/
│   └── edutrack_schema.sql          # Complete 12-table DDL & seed dataset
└── src/
    ├── database/                    # Connection management
    │   └── DBConnection.java        # JDBC connection factory & diagnostics
    ├── exception/                   # Domain exceptions
    │   ├── DuplicateRecordException.java
    │   ├── EligibilityException.java
    │   ├── InvalidDataException.java
    │   └── RecordNotFoundException.java
    ├── model/                       # 12 Domain entity models
    │   ├── Application.java
    │   ├── AuditLog.java
    │   ├── ClassEntity.java
    │   ├── ClassEnrollment.java
    │   ├── ClassProgressLog.java
    │   ├── ClassResource.java
    │   ├── Company.java
    │   ├── PlacementDrive.java
    │   ├── Quiz.java
    │   ├── QuizSubmission.java
    │   ├── Student.java
    │   └── Teacher.java
    ├── dao/                         # 12 Data Access Objects
    │   ├── ApplicationDAO.java
    │   ├── AuditLogDAO.java
    │   ├── ClassDAO.java
    │   ├── ClassEnrollmentDAO.java
    │   ├── ClassProgressLogDAO.java
    │   ├── ClassResourceDAO.java
    │   ├── CompanyDAO.java
    │   ├── PlacementDriveDAO.java
    │   ├── QuizDAO.java
    │   ├── QuizSubmissionDAO.java
    │   ├── StudentDAO.java
    │   └── TeacherDAO.java
    ├── service/                     # Business logic layer
    │   ├── AuditService.java
    │   ├── ClassService.java
    │   ├── PlacementService.java
    │   ├── QuizService.java
    │   ├── StudentService.java
    │   └── TeacherService.java
    ├── util/                        # Verification runners & console utilities
    │   ├── InputValidator.java
    │   ├── MenuUtils.java
    │   ├── Step2DAOTest.java
    │   ├── Step3TransactionAndDAOTest.java
    │   ├── Step4ServiceAndConcurrencyTest.java
    │   └── Step5EndToEndTest.java
    └── main/                        # Presentation menus & entry point
        ├── AdminMenu.java
        ├── EduTrackApp.java
        ├── StudentMenu.java
        └── TeacherMenu.java
```
