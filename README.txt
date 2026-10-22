ATM MANAGEMENT SYSTEM — JAVA SWING + MYSQL
===========================================

This is a desktop GUI version of the educational ATM project. It reuses the same database schema and business logic as the console version.

FEATURES
--------
- GUI login using card number and PIN
- Create demo account
- Check balance
- Deposit money
- Withdraw money with insufficient-balance protection
- Mini statement in a table
- Change PIN
- PINs stored as salted PBKDF2 hashes
- Prepared SQL statements and transactional balance updates

REQUIREMENTS
------------
- JDK 17 or later
- MySQL Server and MySQL Workbench
- Maven
- VS Code with Extension Pack for Java (recommended)

STEP 1 — DATABASE
-----------------
1. Open MySQL Workbench and connect to your local server.
2. Open sql/atm_schema.sql and execute it.
3. Confirm the atm_management schema contains accounts and transactions.
If you already created these tables for the console version, you do not need to recreate them.

STEP 2 — PASSWORD
-----------------
Open src/main/java/com/atm/Database.java.
Replace YOUR_MYSQL_PASSWORD with your local MySQL root password.
Alternatively set the ATM_DB_PASSWORD environment variable before starting Maven.

STEP 3 — OPEN PROJECT
---------------------
Extract this ZIP. In VS Code, select File > Open Folder and open the folder named ATM_Management_System_Swing that contains pom.xml.
Do not open the parent folder if it contains another nested project folder.

STEP 4 — COMPILE
----------------
Open Terminal > New Terminal. Confirm the terminal is in the folder containing pom.xml.
Run:
    mvn compile

Expected result:
    BUILD SUCCESS

STEP 5 — RUN GUI
----------------
Run:
    mvn exec:java "-Dexec.mainClass=com.atm.SwingMain"

A desktop window named ATM MANAGEMENT SYSTEM should open.
You can also run Main.java; it opens the same Swing interface.

STEP 6 — TEST
-------------
1. Select Create Demo Account.
2. Enter a name, 8–20 digit card number, 4–6 digit PIN, and opening balance.
3. Log in.
4. Test balance, deposit, withdrawal, mini statement and PIN change.
5. Verify saved data in MySQL Workbench:
       USE atm_management;
       SELECT * FROM accounts;
       SELECT * FROM transactions ORDER BY created_at DESC;

COMMON ISSUES
-------------
- "mvn is not recognized": install Maven or configure its PATH.
- "No POM in this directory": change directory to the folder containing pom.xml.
- "Access denied for user": correct the password in Database.java or set ATM_DB_PASSWORD.
- "Unknown database": execute sql/atm_schema.sql first.
- Card number duplicate: use a different card number or log in to the existing demo account.

SECURITY NOTICE
---------------
This is a student/demo application, not production banking software. It does not connect to real bank networks or cash dispensers. Production ATMs require secure hardware, strict login attempt limits, account lockouts, key management, encrypted connections, audit controls, compliance checks and specialist security review. Do not use real banking credentials or customer information.
