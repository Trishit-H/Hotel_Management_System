import java.sql.*;
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;

/**
 * Database connection using SQLite.
 * Handles all table creation including users for authentication.
 */
public class DBConnection {
    private static final String URL = "jdbc:sqlite:hotel.db";
    private static final String USER = "";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * SHA-256 hash utility for passwords.
     * Never store plain-text passwords.
     */
    // public static String hashPassword(String password) {
    // try {
    // MessageDigest md = MessageDigest.getInstance("SHA-256");
    // byte[] bytes = md.digest(password.getBytes());
    // StringBuilder sb = new StringBuilder();
    // for (byte b : bytes)
    // sb.append(String.format("%02x", b));
    // return sb.toString();
    // } catch (NoSuchAlgorithmException e) {
    // throw new RuntimeException("SHA-256 not available", e);
    // }
    // }

    public static void initDatabase() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            // Rooms table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            room_number TEXT NOT NULL UNIQUE,
                            room_type TEXT NOT NULL,
                            price REAL NOT NULL,
                            status TEXT DEFAULT 'Available'
                        )
                    """);

            // Customers table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS customers (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            contact TEXT NOT NULL,
                            address TEXT
                        )
                    """);

            // Bookings table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS bookings (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            room_id INTEGER NOT NULL,
                            customer_id INTEGER NOT NULL,
                            check_in TEXT NOT NULL,
                            check_out TEXT NOT NULL,
                            total_amount REAL NOT NULL,
                            FOREIGN KEY (room_id) REFERENCES rooms(id),
                            FOREIGN KEY (customer_id) REFERENCES customers(id)
                        )
                    """);

            // Users table for authentication
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username TEXT NOT NULL UNIQUE,
                            password_hash TEXT NOT NULL,
                            role TEXT NOT NULL DEFAULT 'staff'
                        )
                    """);

            // Sample rooms if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.execute(
                        "INSERT INTO rooms (room_number, room_type, price, status) VALUES ('101', 'AC', 1500, 'Available')");
                stmt.execute(
                        "INSERT INTO rooms (room_number, room_type, price, status) VALUES ('102', 'AC', 1500, 'Available')");
                stmt.execute(
                        "INSERT INTO rooms (room_number, room_type, price, status) VALUES ('103', 'Non-AC', 800, 'Available')");
                stmt.execute(
                        "INSERT INTO rooms (room_number, room_type, price, status) VALUES ('104', 'Non-AC', 800, 'Available')");
                stmt.execute(
                        "INSERT INTO rooms (room_number, room_type, price, status) VALUES ('105', 'Deluxe', 2500, 'Available')");
            }

            // Default users if empty: admin/admin and staff/staff
            ResultSet ur = stmt.executeQuery("SELECT COUNT(*) FROM users");
            ur.next();
            if (ur.getInt(1) == 0) {
                // String adminHash = hashPassword("admin");
                // String staffHash = hashPassword("staff");
                String adminPassword = "admin";
                String staffPassword = "staff";
                stmt.execute("INSERT INTO users (username, password_hash, role) VALUES ('admin', '" + adminPassword
                        + "', 'admin')");
                stmt.execute("INSERT INTO users (username, password_hash, role) VALUES ('staff', '" + staffPassword
                        + "', 'staff')");
                System.out.println("Default users created: admin/admin and staff/staff");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
