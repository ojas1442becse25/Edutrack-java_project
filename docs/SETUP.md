# EduTrack — Installation, Setup & Execution Guide

This guide provides instructions for configuring MySQL 8.0, compiling the source code, and running the **EduTrack** application across Windows, macOS, and Linux platforms.

---

## 1. System Requirements

* **Java Development Kit (JDK)**: Java 8 or higher (Java 11, 17, 21, or 27 recommended). Verify via:
  ```bash
  javac -version
  java -version
  ```
* **MySQL Database**: MySQL Server 8.0+ running on local port `3306`.
* **JDBC Connector**: `lib/mysql-connector-java-8.0.33.jar` (packaged in the `lib/` directory).

---

## 2. MySQL Database Schema Initialization

Open a terminal or command prompt and execute the schema DDL script into your MySQL instance:

### Windows (PowerShell or Command Prompt):
```powershell
# Using cmd.exe redirection
cmd /c '"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot < "d:\Edutrack\sql\edutrack_schema.sql"'
```
*Or using the MySQL interactive client:*
```sql
mysql -u root -p
mysql> SOURCE d:/Edutrack/sql/edutrack_schema.sql;
```

### macOS / Linux:
```bash
mysql -u root -p < sql/edutrack_schema.sql
```

This creates the database `edutrack_db`, sets character set `utf8mb4`, deploys all 12 tables, enforces foreign key cascade constraints, creates performance indexes, and populates 3 test records per table.

---

## 3. Database Credentials Configuration

By default, [DBConnection.java](file:///d:/Edutrack/src/database/DBConnection.java) connects using:
* **Host**: `localhost`
* **Port**: `3306`
* **Database**: `edutrack_db`
* **User**: `root`
* **Password**: `root`

### Custom Credentials:
You can supply your MySQL credentials at runtime without modifying Java code via System Properties:
```bash
java -Ddb.user=your_username -Ddb.password=your_password -cp "bin;lib/mysql-connector-java-8.0.33.jar" main.EduTrackApp
```
Or via Environment Variables:
```bash
export EDUTRACK_DB_USER="your_user"
export EDUTRACK_DB_PASSWORD="your_password"
```

---

## 4. Compilation Instructions

Compile all project layers from the root repository directory (`d:\Edutrack`):

### Windows (PowerShell / Command Prompt):
```powershell
javac -d bin -cp "bin;lib/mysql-connector-java-8.0.33.jar" src/exception/*.java src/model/*.java src/database/*.java src/dao/*.java src/service/*.java src/util/*.java src/main/*.java
```

### macOS / Linux:
```bash
javac -d bin -cp "bin:lib/mysql-connector-java-8.0.33.jar" src/exception/*.java src/model/*.java src/database/*.java src/dao/*.java src/service/*.java src/util/*.java src/main/*.java
```

---

## 5. Running the Application

Launch the main console interactive application:

### Windows:
```powershell
java -cp "bin;lib/mysql-connector-java-8.0.33.jar" main.EduTrackApp
```

### macOS / Linux:
```bash
java -cp "bin:lib/mysql-connector-java-8.0.33.jar" main.EduTrackApp
```

---

## 6. Seed Credentials for Quick Evaluation

You can use the following pre-loaded credentials to test each portal immediately:

| Portal | Login Identifier | Seed User Name | Context Details |
|---|---|---|---|
| **Faculty Portal** | `alan.turing@edutrack.edu` | Dr. Alan Turing | Assigned to `CS301` (DBMS) |
| **Faculty Portal** | `grace.hopper@edutrack.edu` | Dr. Grace Hopper | Assigned to `CS302` (OOP Java) |
| **Student Portal** | `CS2023001` | Rahul Sharma | Sem 6, CGPA 8.75 |
| **Student Portal** | `CS2023002` | Priya Nair | Sem 6, CGPA 9.20 |
| **Student Portal** | `IT2023003` | Aman Verma | Sem 6, CGPA 7.40 |
| **Admin Portal** | Direct Selection (Option 3) | Academic & Placement Admin | Full access |

---

## 7. Running Automated Test Suites

To execute the individual verification suites and regenerate evidence logs:

* **Step 2 (Core DAOs & Domain Validation)**:
  ```bash
  java -cp "bin;lib/mysql-connector-java-8.0.33.jar" util.Step2DAOTest
  ```
* **Step 3 (Atomic Commit & Rollback Transactions)**:
  ```bash
  java -cp "bin;lib/mysql-connector-java-8.0.33.jar" util.Step3TransactionAndDAOTest
  ```
* **Step 4 (Service Layer & Multi-Threaded Concurrency)**:
  ```bash
  java -cp "bin;lib/mysql-connector-java-8.0.33.jar" util.Step4ServiceAndConcurrencyTest
  ```
* **Step 5 (Full End-to-End Workflow Integration)**:
  ```bash
  java -cp "bin;lib/mysql-connector-java-8.0.33.jar" util.Step5EndToEndTest
  ```
