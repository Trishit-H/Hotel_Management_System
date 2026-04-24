import javax.swing.SwingUtilities;

/**
 * Hotel Management System - Entry Point
 * Shows login dialog first. Only launches HotelApp on successful
 * authentication.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // 1. Initialize database (creates tables + default users if needed)
            DBConnection.initDatabase();

            // 2. Show login dialog (modal - blocks until dismissed)
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);

            // 3. If authenticated, open main application
            if (login.isAuthenticated()) {
                HotelApp app = new HotelApp(login.getLoggedInUser(), login.getLoggedInRole());
                app.setVisible(true);
            } else {
                // User closed the dialog without logging in - exit
                System.exit(0);
            }
        });
    }
}
