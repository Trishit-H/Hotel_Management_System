import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Hotel Management System - Main Application Window.
 * Requires authentication via LoginDialog before opening.
 * Role 'admin' gets full access; role 'staff' cannot add/delete rooms.
 */
public class HotelApp extends JFrame {

    // Database
    private Connection conn;

    // Logged-in user info
    private final String currentUser;
    private final String currentRole;

    // UI Components
    private JTabbedPane tabs;
    private JTable roomTable, customerTable, bookingTable;
    private DefaultTableModel roomModel, customerModel, bookingModel;

    public HotelApp(String username, String role) {
        this.currentUser = username;
        this.currentRole = role;

        setTitle("Hotel Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
        // setSize(960, 640);
        setLocationRelativeTo(null);

        initDatabase();
        buildUI();
        loadData();
    }

    private void initDatabase() {
        try {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(this,
                        "SQLite Driver not found! Please add sqlite-jdbc.jar to lib folder.");
                throw new RuntimeException(e);
            }

            conn = DriverManager.getConnection("jdbc:sqlite:hotel.db");

            var stmt = conn.createStatement();
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            room_number TEXT NOT NULL UNIQUE,
                            room_type TEXT NOT NULL,
                            price REAL NOT NULL,
                            status TEXT DEFAULT 'Available'
                        )
                    """);
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS customers (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            contact TEXT NOT NULL,
                            address TEXT
                        )
                    """);
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

            var rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms");
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
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildUI() {
        // ------ Top bar: user info + logout
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(30, 60, 114));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        JLabel appLabel = new JLabel("🏨 Hotel Management System");
        appLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        appLabel.setForeground(Color.WHITE);

        // Role badge color
        Color roleBg = currentRole.equals("admin") ? new Color(255, 165, 0) : new Color(100, 160, 230);
        JLabel userLabel = new JLabel("  👤 " + currentUser + "  [" + currentRole.toUpperCase() + "]  ");
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        userLabel.setForeground(Color.WHITE);
        userLabel.setOpaque(true);
        userLabel.setBackground(roleBg);
        userLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        logoutBtn.setBackground(new Color(220, 60, 60));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setOpaque(true);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(userLabel);
        rightPanel.add(logoutBtn);

        topBar.add(appLabel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);

        // --- Tabs
        tabs = new JTabbedPane();
        tabs.addTab("🛏 Rooms", buildRoomPanel());
        tabs.addTab("👤 Customers", buildCustomerPanel());
        tabs.addTab("📋 Bookings", buildBookingPanel());

        // Admin-only: Manage Users tab
        if (currentRole.equals("admin")) {
            tabs.addTab("🔐 Users", buildUserManagementPanel());
        }

        // --- Layout
        setLayout(new BorderLayout());
        add(topBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            // Re-launch login
            SwingUtilities.invokeLater(() -> {
                DBConnection.initDatabase();
                LoginDialog login = new LoginDialog(null);
                login.setVisible(true);
                if (login.isAuthenticated()) {
                    new HotelApp(login.getLoggedInUser(), login.getLoggedInRole()).setVisible(true);
                } else {
                    System.exit(0);
                }
            });
        }
    }

    // ========================================================================
    // ROOMS PANEL
    // ========================================================================
    private JPanel buildRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = { "ID", "Room No", "Type", "Price", "Status" };
        roomModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        roomTable = new JTable(roomModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(roomTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Room");
        JButton delBtn = new JButton("Delete");

        addBtn.addActionListener(e -> addRoom());
        delBtn.addActionListener(e -> deleteRoom());

        // Only admins can add/delete rooms
        if (!currentRole.equals("admin")) {
            addBtn.setEnabled(false);
            addBtn.setToolTipText("Admin access required");
            delBtn.setEnabled(false);
            delBtn.setToolTipText("Admin access required");
        }

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addRoom() {
        JTextField no = new JTextField();
        JTextField type = new JTextField();
        JTextField price = new JTextField();

        Object[] msg = { "Room Number:", no, "Type (AC/Non-AC/Deluxe):", type, "Price:", price };
        int opt = JOptionPane.showConfirmDialog(this, msg, "Add Room", JOptionPane.OK_CANCEL_OPTION);

        if (opt == JOptionPane.OK_OPTION) {
            try {
                var ps = conn.prepareStatement(
                        "INSERT INTO rooms (room_number, room_type, price, status) VALUES (?, ?, ?, 'Available')");
                ps.setString(1, no.getText());
                ps.setString(2, type.getText());
                ps.setDouble(3, Double.parseDouble(price.getText()));
                ps.execute();
                loadData();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void deleteRoom() {
        int row = roomTable.getSelectedRow();
        if (row >= 0) {
            int id = (int) roomModel.getValueAt(row, 0);
            try {
                var ps = conn.prepareStatement("DELETE FROM rooms WHERE id = ?");
                ps.setInt(1, id);
                ps.execute();
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    // ===============================================================
    // CUSTOMERS PANEL
    // ===============================================================
    private JPanel buildCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = { "ID", "Name", "Contact", "Address" };
        customerModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        customerTable = new JTable(customerModel);
        panel.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Customer");
        JButton delBtn = new JButton("Delete");

        addBtn.addActionListener(e -> addCustomer());
        delBtn.addActionListener(e -> deleteCustomer());

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addCustomer() {
        JTextField name = new JTextField();
        JTextField contact = new JTextField();
        JTextField address = new JTextField();

        Object[] msg = { "Name:", name, "Contact:", contact, "Address:", address };
        int opt = JOptionPane.showConfirmDialog(this, msg, "Add Customer", JOptionPane.OK_CANCEL_OPTION);

        if (opt == JOptionPane.OK_OPTION) {
            try {
                var ps = conn.prepareStatement("INSERT INTO customers (name, contact, address) VALUES (?, ?, ?)");
                ps.setString(1, name.getText());
                ps.setString(2, contact.getText());
                ps.setString(3, address.getText());
                ps.execute();
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void deleteCustomer() {
        int row = customerTable.getSelectedRow();
        if (row >= 0) {
            int id = (int) customerModel.getValueAt(row, 0);
            try {
                var ps = conn.prepareStatement("DELETE FROM customers WHERE id = ?");
                ps.setInt(1, id);
                ps.execute();
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    // ===============================================================
    // BOOKINGS PANEL
    // ===============================================================
    private JPanel buildBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = { "ID", "Room", "Customer", "Check-In", "Check-Out", "Amount" };
        bookingModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        bookingTable = new JTable(bookingModel);
        panel.add(new JScrollPane(bookingTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("New Booking");
        JButton delBtn = new JButton("Check-Out");

        addBtn.addActionListener(e -> newBooking());
        delBtn.addActionListener(e -> checkOut());

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void newBooking() {
        try {
            var rooms = conn.prepareStatement("SELECT id, room_number, price FROM rooms WHERE status = 'Available'");
            var rs = rooms.executeQuery();
            java.util.List<String[]> roomList = new java.util.ArrayList<>();
            while (rs.next())
                roomList.add(new String[] { rs.getString(1), rs.getString(2), rs.getString(3) });

            if (roomList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No rooms available!");
                return;
            }

            JComboBox<String> roomBox = new JComboBox<>();
            for (String[] r : roomList)
                roomBox.addItem(r[1] + " - ₹" + r[2]);

            var custs = conn.prepareStatement("SELECT id, name FROM customers");
            var cr = custs.executeQuery();
            java.util.List<String[]> custList = new java.util.ArrayList<>();
            JComboBox<String> custBox = new JComboBox<>();
            while (cr.next()) {
                custList.add(new String[] { cr.getString(1), cr.getString(2) });
                custBox.addItem(cr.getString(2));
            }

            if (custList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Add a customer first!");
                return;
            }

            JTextField checkIn = new JTextField(LocalDate.now().toString());
            JTextField checkOut = new JTextField(LocalDate.now().plusDays(1).toString());

            Object[] msg = { "Room:", roomBox, "Customer:", custBox, "Check-In (YYYY-MM-DD):", checkIn, "Check-Out:",
                    checkOut };
            int opt = JOptionPane.showConfirmDialog(this, msg, "New Booking", JOptionPane.OK_CANCEL_OPTION);

            if (opt == JOptionPane.OK_OPTION) {
                int ri = roomBox.getSelectedIndex();
                int ci = custBox.getSelectedIndex();

                double price = Double.parseDouble(roomList.get(ri)[2]);
                long days = ChronoUnit.DAYS.between(LocalDate.parse(checkIn.getText()),
                        LocalDate.parse(checkOut.getText()));
                double total = price * Math.max(days, 1);

                var ps = conn.prepareStatement(
                        "INSERT INTO bookings (room_id, customer_id, check_in, check_out, total_amount) VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, Integer.parseInt(roomList.get(ri)[0]));
                ps.setInt(2, Integer.parseInt(custList.get(ci)[0]));
                ps.setString(3, checkIn.getText());
                ps.setString(4, checkOut.getText());
                ps.setDouble(5, total);
                ps.execute();

                var up = conn.prepareStatement("UPDATE rooms SET status = 'Booked' WHERE id = ?");
                up.setInt(1, Integer.parseInt(roomList.get(ri)[0]));
                up.execute();

                loadData();
            }
        } catch (SQLException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void checkOut() {
        int row = bookingTable.getSelectedRow();
        if (row >= 0) {
            int id = (int) bookingModel.getValueAt(row, 0);
            try {
                var ps = conn.prepareStatement("SELECT room_id FROM bookings WHERE id = ?");
                ps.setInt(1, id);
                var rs = ps.executeQuery();
                if (rs.next()) {
                    int roomId = rs.getInt(1);
                    var del = conn.prepareStatement("DELETE FROM bookings WHERE id = ?");
                    del.setInt(1, id);
                    del.execute();
                    var up = conn.prepareStatement("UPDATE rooms SET status = 'Available' WHERE id = ?");
                    up.setInt(1, roomId);
                    up.execute();
                    loadData();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    // ===============================================================
    // USER MANAGEMENT PANEL (Admin only)
    // ===============================================================
    private JPanel buildUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = { "ID", "Username", "Role" };
        DefaultTableModel userModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable userTable = new JTable(userModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Load users
        Runnable loadUsers = () -> {
            userModel.setRowCount(0);
            try (var stmt = conn.createStatement()) {
                var rs = stmt.executeQuery("SELECT id, username, role FROM users");
                while (rs.next()) {
                    userModel.addRow(new Object[] { rs.getInt(1), rs.getString(2), rs.getString(3) });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
        loadUsers.run();

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addUserBtn = new JButton("Add User");
        addUserBtn.addActionListener(e -> {
            JTextField uname = new JTextField();
            JPasswordField pass = new JPasswordField();
            String[] roles = { "staff", "admin" };
            JComboBox<String> roleBox = new JComboBox<>(roles);

            Object[] msg = { "Username:", uname, "Password:", pass, "Role:", roleBox };
            int opt = JOptionPane.showConfirmDialog(panel, msg, "Add User", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                try {
                    String hash = new String(pass.getPassword());
                    var ps = conn
                            .prepareStatement("INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)");
                    ps.setString(1, uname.getText().trim());
                    ps.setString(2, hash);
                    ps.setString(3, (String) roleBox.getSelectedItem());
                    ps.execute();
                    loadUsers.run();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(panel, "Error: " + ex.getMessage());
                }
            }
        });

        JButton delUserBtn = new JButton("Delete User");
        delUserBtn.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row >= 0) {
                String uname = (String) userModel.getValueAt(row, 1);
                if (uname.equals(currentUser)) {
                    JOptionPane.showMessageDialog(panel, "You cannot delete your own account.");
                    return;
                }
                int id = (int) userModel.getValueAt(row, 0);
                try {
                    var ps = conn.prepareStatement("DELETE FROM users WHERE id = ?");
                    ps.setInt(1, id);
                    ps.execute();
                    loadUsers.run();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(panel, "Error: " + ex.getMessage());
                }
            }
        });

        JButton changePassBtn = new JButton("Change Password");
        changePassBtn.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row >= 0) {
                int id = (int) userModel.getValueAt(row, 0);
                JPasswordField newPass = new JPasswordField();
                int opt = JOptionPane.showConfirmDialog(panel,
                        new Object[] { "New Password:", newPass }, "Change Password", JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.OK_OPTION) {
                    try {
                        String hash = new String(newPass.getPassword());
                        var ps = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?");
                        ps.setString(1, hash);
                        ps.setInt(2, id);
                        ps.execute();
                        JOptionPane.showMessageDialog(panel, "Password updated successfully.");
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(panel, "Error: " + ex.getMessage());
                    }
                }
            }
        });

        btnPanel.add(addUserBtn);
        btnPanel.add(delUserBtn);
        btnPanel.add(changePassBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ===============================================================
    // LOAD DATA
    // ===============================================================
    private void loadData() {
        try {
            roomModel.setRowCount(0);
            var rs = conn.createStatement().executeQuery("SELECT * FROM rooms");
            while (rs.next()) {
                roomModel.addRow(new Object[] { rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4),
                        rs.getString(5) });
            }

            customerModel.setRowCount(0);
            var cs = conn.createStatement().executeQuery("SELECT * FROM customers");
            while (cs.next()) {
                customerModel.addRow(new Object[] { cs.getInt(1), cs.getString(2), cs.getString(3), cs.getString(4) });
            }

            bookingModel.setRowCount(0);
            var bs = conn.createStatement().executeQuery("""
                        SELECT b.id, r.room_number, c.name, b.check_in, b.check_out, b.total_amount
                        FROM bookings b
                        JOIN rooms r ON b.room_id = r.id
                        JOIN customers c ON b.customer_id = c.id
                    """);
            while (bs.next()) {
                bookingModel.addRow(new Object[] { bs.getInt(1), bs.getString(2), bs.getString(3), bs.getString(4),
                        bs.getString(5), bs.getDouble(6) });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
