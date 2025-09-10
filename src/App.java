// App.java
import controller.AuthController;
import controller.CustomerController;
import model.entity.User;
import model.service.AuthService;
import util.DatabaseInitUtil;

/**
 * Banking System Application
 * @author Group 2 - ISTAD
 * @version 1.07.2025
 * @since 2025
 */
public class App {
    private final AuthController authController;
    private final CustomerController customerController;
    private final AuthService authService;

    public App() {
        this.authController = new AuthController();
        this.customerController = new CustomerController();
        this.authService = new AuthService();
    }


    /**
     * Initialize the application
     */
    public void init() {
        System.out.println("🏦 Initializing ISTAD Banking System...");

        // Initialize database with required data
        DatabaseInitUtil dbInit = new DatabaseInitUtil();
        dbInit.initializeAll();

        // Create default admin if not exists
        createDefaultAdmin();

        System.out.println("✅ System initialized successfully!");
        System.out.println("💡 You can now:");
        System.out.println("   • Login with existing account");
        System.out.println("   • Register new customer account");
        System.out.println("   • Use default admin (username: admin, password: Admin123!)");
    }

    /**
     * Start the application main loop
     */
    public void start() {
        System.out.println("🚀 Starting ISTAD Banking System...\n");

        while (true) {
            try {
                // Handle authentication
                User authenticatedUser = authController.handleAuthentication();

                if (authenticatedUser != null) {
                    // Route user based on role
                    if (authenticatedUser.getRole() == User.Role.ADMIN) {
                        handleAdminFlow(authenticatedUser);
                    } else if (authenticatedUser.getRole() == User.Role.CUSTOMER) {
                        handleCustomerFlow(authenticatedUser);
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();

                // Ask user if they want to continue
                System.out.println("\nPress Enter to continue or Ctrl+C to exit...");
                try {
                    System.in.read();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Handle admin flow (placeholder for now)
     */
    private void handleAdminFlow(User admin) {
        System.out.println("\n🔧 ADMIN PANEL");
        System.out.println("Admin features coming in Phase 6!");
        System.out.println("For now, you can:");
        System.out.println("1. Test unlock account feature");

        // Simple admin menu for testing
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.println("\nOptions:");
        System.out.println("1. Unlock Account");
        System.out.println("0. Logout");
        System.out.print("Choose option: ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice == 1) {
                authController.handleUnlockAccount();
            } else if (choice == 0) {
                authController.logout();
            }
        } catch (Exception e) {
            System.out.println("Invalid input");
        }
    }

    /**
     * Handle customer flow (now fully implemented!)
     */
    private void handleCustomerFlow(User customer) {
        System.out.println("\n🏦 CUSTOMER PORTAL");

        // Pass control to CustomerController
        boolean continueSession = customerController.handleCustomerFlow(customer, authController);

        if (!continueSession) {
            // Customer chose to logout
            authController.logout();
        }

        // Clear customer data when session ends
        customerController.clearCurrentCustomer();
    }

    /**
     * Create default admin user
     */
    private void createDefaultAdmin() {
        System.out.println("🔐 Setting up default admin...");

        boolean adminCreated = authService.createAdmin("admin", "Admin123!");

        if (adminCreated) {
            System.out.println("✅ Default admin created successfully!");
        } else {
            System.out.println("ℹ️ Default admin already exists.");
        }
    }

    /**
     * Shutdown the application gracefully
     */
    public void shutdown() {
        System.out.println("🔒 Shutting down ISTAD Banking System...");

        if (authController.isLoggedIn()) {
            authController.logout();
        }

        System.out.println("✅ System shutdown complete. Goodbye!");
        System.exit(0);
    }

    /**
     * Main method - Entry point
     */
    public static void main(String[] args) {
        App app = new App();

        // Add shutdown hook for clean exit
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));

        try {
            // Initialize system
            app.init();

            // Start application
            app.start();

        } catch (Exception e) {
            System.err.println("💥 Fatal error occurred:");
            e.printStackTrace();

            // Ensure clean shutdown
            app.shutdown();
        }
    }
}