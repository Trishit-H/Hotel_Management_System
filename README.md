# Hotel Management System
Pure Java · Swing · Oracle XE · JDBC

---

## Prerequisites
- JDK 8 or above
- Oracle Database XE installed and running
- `ojdbc11.jar` (Oracle JDBC driver)

---

## Setup Steps

### 1. Database
Open SQL*Plus / SQL Developer and run:
```
@setup.sql
```

### 2. Configure credentials
Edit `src/DBConnection.java`:
```java
private static final String URL      = "jdbc:oracle:thin:@localhost:1521:XE";
private static final String USER     = "your_username";
private static final String PASSWORD = "your_password";
```

### 3. Project structure
```
HotelManagementSystem/
├── src/
│   ├── Main.java
│   ├── LoginPage.java
│   ├── Dashboard.java
│   ├── Theme.java
│   ├── UIFactory.java
│   ├── Models.java          ← Room, Customer, Booking
│   ├── DBConnection.java
│   ├── LoggerUtil.java
│   ├── RoomDAO.java
│   ├── CustomerDAO.java
│   ├── BookingDAO.java
│   ├── Billing.java
│   ├── RoomPanel.java
│   ├── CustomerPanel.java
│   ├── BookingPanel.java
│   ├── BillingPanel.java
│   └── ReportsPanel.java
├── lib/
│   └── ojdbc11.jar
└── setup.sql
```

### 4. Compile (PowerShell)
```powershell
cd HotelManagementSystem
javac -cp "lib/ojdbc11.jar" -d out (Get-ChildItem src -Filter *.java | % { $_.FullName })
```

Or in CMD:
```cmd
javac -cp "lib\ojdbc11.jar" -d out src\*.java
```

### 5. Run
```powershell
java -cp "out;lib/ojdbc11.jar" Main
```

---

## Default Login
- **Username:** `admin`
- **Password:** `admin`

(Hardcoded in `LoginPage.java`)

---

## IDE Setup (NetBeans / Eclipse)
1. Create a new Java Application project
2. Copy all `.java` files from `src/` into the project's source package
3. Add `ojdbc11.jar` to the project's Libraries
4. Set `Main` as the main class
5. Run

---

## Modules
| Module       | Description                              |
|--------------|------------------------------------------|
| Login        | Authenticates the user                   |
| Dashboard    | Sidebar navigation hub                   |
| Rooms        | Add / update / delete / view rooms       |
| Customers    | Manage guest records                     |
| Bookings     | Assign rooms, set dates, auto-calculate  |
| Billing      | View bill preview per booking            |
| Reports      | Full booking history with search filter  |
    