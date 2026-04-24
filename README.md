# Hotel Management System
Pure Java · Swing · SQLite · JDBC

---

## Prerequisites
- JDK 8 or above
- `sqlite-jdbc-3.53.0.0.jar` (SQLite JDBC driver)

---

## Setup Steps

### 1. Database
Add SQLite jar file to the lib folder


### 2. Configure credentials
Edit `src/DBConnection.java`:
```java
private static final String URL      = "jdbc:sqlite:hotel.db";
private static final String USER     = "";
private static final String PASSWORD = "";
```

### 3. Project structure
```
HotelManagementSystem/
├── src/
│   ├── Main.java
│   ├── LoginDialog.java
│   ├── DBConnection.java
│   ├── HotelApp.java
├── lib/
│   └── ojdbc11.jar
└── README.md
```

### 4. Compile (PowerShell)
```powershell
cd HotelManagementSystem
javac -cp "lib/sqlite-jdbc-3.53.0.0.jar" -d out (Get-ChildItem src -Filter *.java | % { $_.FullName })
```

Or in CMD:
```cmd
javac -cp "lib\sqlite-jdbc-3.53.0.0.jar" -d out src\*.java
```

### 5. Run
```powershell
java -cp "out;lib/sqlite-jdbc-3.53.0.0.jar" Main
```

---

## Default Login
- **Username:** `admin`
- **Password:** `admin`

(Hardcoded in `LoginPage.java`)

---
    