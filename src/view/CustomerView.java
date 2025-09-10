// view/CustomerView.java
package view;

import model.entity.Customer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class CustomerView {
    private final Scanner scanner;

    public CustomerView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Get customer profile information from user input
     */
    public Customer getCustomerProfileData() {
        System.out.println("\n=== CREATE CUSTOMER PROFILE ===");
        System.out.println("To start using banking services, please complete your profile:");
        System.out.println();

        // Full Name
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine().trim();

        // Date of Birth
        LocalDate dob = null;
        while (dob == null) {
            System.out.print("Date of Birth (YYYY-MM-DD): ");
            String dobInput = scanner.nextLine().trim();
            try {
                dob = LocalDate.parse(dobInput, DateTimeFormatter.ISO_LOCAL_DATE);

                // Check if user is at least 18
                int age = java.time.Period.between(dob, LocalDate.now()).getYears();
                if (age < 18) {
                    System.out.println("❌ You must be at least 18 years old to open a bank account!");
                    dob = null;
                    continue;
                }

                System.out.println("✅ Age: " + age + " years old");

            } catch (DateTimeParseException e) {
                System.out.println("❌ Invalid date format! Please use YYYY-MM-DD format (e.g., 1990-01-15).");
            }
        }

        // Gender
        System.out.println("\nGender options: Male, Female, Other");
        System.out.print("Gender: ");
        String gender = scanner.nextLine().trim();

        // Address Information
        System.out.println("\n--- Address Information ---");
        System.out.print("Street Address: ");
        String address = scanner.nextLine().trim();

        System.out.print("City/Province: ");
        String cityOrProvince = scanner.nextLine().trim();

        System.out.print("Country: ");
        String country = scanner.nextLine().trim();
        if (country.trim().isEmpty()) {
            country = "Cambodia"; // Default
            System.out.println("(Defaulted to: Cambodia)");
        }

        System.out.print("Zip Code (optional): ");
        String zipCode = scanner.nextLine().trim();
        if (zipCode.isEmpty()) zipCode = null;

        // Contact Information
        System.out.println("\n--- Contact Information ---");
        System.out.print("Phone Number (e.g., 012345678): ");
        String phoneNumber = scanner.nextLine().trim();

        System.out.print("Email Address: ");
        String email = scanner.nextLine().trim();

        // Employment Information
        System.out.println("\n--- Employment Information ---");
        System.out.println("Employment types: Full-time, Part-time, Self-employed, Student, Unemployed, Retired");
        System.out.print("Employment Type: ");
        String employmentType = scanner.nextLine().trim();

        System.out.print("Company Name (if applicable): ");
        String companyName = scanner.nextLine().trim();
        if (companyName.isEmpty()) companyName = null;

        System.out.print("Position/Job Title (if applicable): ");
        String position = scanner.nextLine().trim();
        if (position.isEmpty()) position = null;

        System.out.print("Main Source of Income: ");
        String mainSourceOfIncome = scanner.nextLine().trim();

        // Monthly Income Range
        System.out.println("\n--- Income Information ---");
        System.out.println("Select your monthly income range:");
        System.out.println("1. Less than $500");
        System.out.println("2. $500 - $1,000");
        System.out.println("3. $1,001 - $2,000");
        System.out.println("4. $2,001 - $5,000");
        System.out.println("5. More than $5,000");
        System.out.print("Select income range (1-5): ");

        String[] incomeRanges = {
                "Less than $500", "$500 - $1,000", "$1,001 - $2,000",
                "$2,001 - $5,000", "More than $5,000"
        };

        String monthlyIncomeRange = "";
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice >= 1 && choice <= 5) {
                monthlyIncomeRange = incomeRanges[choice - 1];
                System.out.println("Selected: " + monthlyIncomeRange);
            } else {
                monthlyIncomeRange = "Not specified";
                System.out.println("Invalid choice. Set to: Not specified");
            }
        } catch (NumberFormatException e) {
            monthlyIncomeRange = "Not specified";
            System.out.println("Invalid input. Set to: Not specified");
        }

        // Banking PIN
        System.out.println("\n--- Banking Security ---");
        System.out.println("💡 Banking PIN Requirements:");
        System.out.println("• Must be exactly 6 digits");
        System.out.println("• Cannot be all same digits (e.g., 111111)");
        System.out.println("• This PIN will be used for banking transactions");

        String pin = "";
        while (pin.length() != 6 || !isValidPin(pin)) {
            System.out.print("Banking PIN (6 digits): ");
            pin = scanner.nextLine().trim();

            if (pin.length() != 6) {
                System.out.println("❌ PIN must be exactly 6 digits!");
            } else if (!pin.matches("\\d{6}")) {
                System.out.println("❌ PIN must contain only digits!");
            } else if (!isValidPin(pin)) {
                System.out.println("❌ PIN cannot be all same digits (e.g., 111111)!");
            } else {
                System.out.println("✅ PIN accepted");
                break;
            }
        }

        // Optional Remarks
        System.out.print("\nAdditional Remarks (optional): ");
        String remark = scanner.nextLine().trim();
        if (remark.isEmpty()) remark = null;

        return new Customer(fullName, dob, gender, address, cityOrProvince, country,
                zipCode, phoneNumber, email, employmentType, companyName,
                position, mainSourceOfIncome, monthlyIncomeRange, pin);
    }

    /**
     * Validate PIN (no all same digits)
     */
    private boolean isValidPin(String pin) {
        if (pin == null || pin.length() != 6) {
            return false;
        }

        char firstDigit = pin.charAt(0);
        for (char c : pin.toCharArray()) {
            if (c != firstDigit) {
                return true; // At least one different digit found
            }
        }
        return false; // All digits are the same
    }

    /**
     * Display customer profile summary
     */
    public void displayCustomerProfile(Customer customer) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           CUSTOMER PROFILE SUMMARY");
        System.out.println("=".repeat(50));
        System.out.println(customer.getSummary());
        System.out.println("=".repeat(50));
    }

    /**
     * Show customer main menu (placeholder for Phase 3)
     */
    public int showCustomerMainMenu(Customer customer) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("        CUSTOMER BANKING PORTAL");
        System.out.println("        Welcome, " + customer.getDisplayName() + "!");
        System.out.println("=".repeat(50));
        System.out.println("1. Account Management");
        System.out.println("2. Make Transaction");
        System.out.println("3. Pay Bills");
        System.out.println("4. View Transaction History");
        System.out.println("5. Request Account Freeze");
        System.out.println("6. Apply for Loan");
        System.out.println("7. Manage Favorites");
        System.out.println("8. Update Profile");
        System.out.println("0. Logout");
        System.out.print("Choose an option: ");

        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; // Invalid input
        }
    }

    /**
     * Show profile update confirmation
     */
    public boolean confirmProfileUpdate(Customer customer) {
        System.out.println("\n=== CONFIRM PROFILE UPDATE ===");
        displayCustomerProfile(customer);

        System.out.print("\nConfirm profile update? (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }

    /**
     * Display success message
     */
    public void showSuccessMessage(String message) {
        System.out.println("✅ " + message);
    }

    /**
     * Display error message
     */
    public void showErrorMessage(String message) {
        System.out.println("❌ " + message);
    }

    /**
     * Display info message
     */
    public void showInfoMessage(String message) {
        System.out.println("ℹ️ " + message);
    }

    /**
     * Display loading message
     */
    public void showLoading(String message) {
        System.out.println("⏳ " + message + "...");
    }

    /**
     * Wait for user to press Enter
     */
    public void waitForEnter() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Clear screen (simple version)
     */
    public void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    /**
     * Show customer not found message
     */
    public void showCustomerNotFound() {
        System.out.println("\n❌ Customer profile not found!");
        System.out.println("You need to complete your customer profile before accessing banking services.");
    }

    /**
     * Show welcome back message
     */
    public void showWelcomeBack(Customer customer) {
        System.out.println("\n🎉 Welcome back, " + customer.getDisplayName() + "!");
        System.out.println("Your customer profile is complete and ready for banking services.");
    }
}