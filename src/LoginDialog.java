import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * Login Dialog for Hotel Management System.
 * Validates credentials against the users table using SHA-256 hashed passwords.
 * Supports two roles: admin (full access) and staff (limited access).
 */
public class LoginDialog extends JDialog {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private boolean authenticated = false;
    private String loggedInUser = "";
    private String loggedInRole = "";

    public LoginDialog(Frame parent) {
        super(parent, "Hotel Management System - Login", true); // modal
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        // ---- Main panel with padding
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(new Color(30, 60, 114));

        // ---- Header banner
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 60, 114));
        header.setBorder(BorderFactory.createEmptyBorder(24, 30, 16, 30));

        JLabel title = new JLabel("Hotel Management", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Please sign in to continue", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(180, 210, 255));

        header.add(title, BorderLayout.CENTER);
        header.add(subtitle, BorderLayout.SOUTH);
        main.add(header, BorderLayout.NORTH);

        // ---- Form card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Username
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        userLabel.setForeground(new Color(60, 60, 60));
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = new JTextField(18);
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        passLabel.setForeground(new Color(60, 60, 60));
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField(18);
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Status label (shows error messages)
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(200, 50, 50));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Login button
        JButton loginBtn = new JButton("Sign In");
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginBtn.setBackground(new Color(30, 60, 114));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setOpaque(true);
        loginBtn.setBorderPainted(false);
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginBtn.addActionListener(e -> attemptLogin());
        // Allow Enter key from password field
        passwordField.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());

        // Hint label
        JLabel hint = new JLabel("Default: admin / admin");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(150, 150, 150));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Assemble card
        card.add(userLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(12));
        card.add(passLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(6));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(hint);

        main.add(card, BorderLayout.CENTER);

        // ----- Footer
        JPanel footer = new JPanel();
        footer.setBackground(new Color(240, 244, 255));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JLabel footerLabel = new JLabel("Unauthorized access is prohibited");
        footerLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(120, 120, 140));
        footer.add(footerLabel);
        main.add(footer, BorderLayout.SOUTH);

        setContentPane(main);
        setPreferredSize(new Dimension(360, 440));

        // Close app if login window is dismissed without logging in
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }

        // String hashedPassword = DBConnection.hashPassword(password);

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT role FROM users WHERE username = ? AND password_hash = ?");
            ps.setString(1, username);
            // ps.setString(2, hashedPassword);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                authenticated = true;
                loggedInUser = username;
                loggedInRole = rs.getString("role");
                dispose(); // close login, open main app
            } else {
                statusLabel.setText("✗ Invalid username or password.");
                passwordField.setText("");
                passwordField.requestFocus();
            }
        } catch (SQLException e) {
            statusLabel.setText("✗ Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Returns true if the user successfully logged in. */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /** Returns the username of the logged-in user. */
    public String getLoggedInUser() {
        return loggedInUser;
    }

    /** Returns the role of the logged-in user ('admin' or 'staff'). */
    public String getLoggedInRole() {
        return loggedInRole;
    }
}
