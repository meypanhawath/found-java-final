// util/AccountUtil.java
package util;

import java.util.Random;
import java.util.Set;

public class AccountUtil {
    private static final Random random = new Random();

    /**
     * Generate a unique 9-digit account number
     * @param existingAccountNumbers Set of existing account numbers to avoid duplicates
     * @return 9-digit account number as string
     */
    public static String generateAccountNumber(Set<String> existingAccountNumbers) {
        String accountNumber;
        int attempts = 0;
        int maxAttempts = 1000; // Prevent infinite loop

        do {
            // Generate 9 random digits
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                // Ensure first digit is not 0
                if (i == 0) {
                    sb.append(random.nextInt(9) + 1); // 1-9
                } else {
                    sb.append(random.nextInt(10)); // 0-9
                }
            }
            accountNumber = sb.toString();
            attempts++;

            if (attempts >= maxAttempts) {
                throw new RuntimeException("Unable to generate unique account number after " + maxAttempts + " attempts");
            }

        } while (existingAccountNumbers.contains(accountNumber));

        return accountNumber;
    }

    /**
     * Generate account name based on customer name, account type, and currency
     * @param customerFullName Customer's full name
     * @param accountType Account type (Saving, Checking, Fixed)
     * @param currency Currency (USD, KHR)
     * @return Formatted account name
     */
    public static String generateAccountName(String customerFullName, String accountType, String currency) {
        // Handle possessive form
        String possessiveName = customerFullName.endsWith("s") ?
                customerFullName + "'" : customerFullName + "'s";

        return String.format("%s %s Account (%s)", possessiveName, accountType, currency);
    }

    /**
     * Format account number for display (xxx xxx xxx)
     * @param accountNumber 9-digit account number
     * @return Formatted account number
     */
    public static String formatAccountNumber(String accountNumber) {
        if (accountNumber != null && accountNumber.length() == 9) {
            return accountNumber.substring(0, 3) + " " +
                    accountNumber.substring(3, 6) + " " +
                    accountNumber.substring(6, 9);
        }
        return accountNumber;
    }

    /**
     * Remove formatting from account number (for storage)
     * @param formattedAccountNumber Formatted account number (xxx xxx xxx)
     * @return Raw 9-digit account number
     */
    public static String unformatAccountNumber(String formattedAccountNumber) {
        if (formattedAccountNumber != null) {
            return formattedAccountNumber.replaceAll("\\s+", "");
        }
        return formattedAccountNumber;
    }

    /**
     * Validate account number format
     * @param accountNumber Account number to validate
     * @return true if valid 9-digit format
     */
    public static boolean isValidAccountNumber(String accountNumber) {
        return accountNumber != null &&
                accountNumber.matches("\\d{9}") &&
                !accountNumber.startsWith("0");
    }

    /**
     * Get currency symbol
     * @param currency Currency code (USD, KHR)
     * @return Currency symbol
     */
    public static String getCurrencySymbol(String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            return "$";
        } else if ("KHR".equalsIgnoreCase(currency)) {
            return "៛";
        }
        return currency;
    }

    /**
     * Format currency amount
     * @param amount Amount to format
     * @param currency Currency code
     * @return Formatted amount with symbol
     */
    public static String formatCurrency(java.math.BigDecimal amount, String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            return "$" + String.format("%,.2f", amount);
        } else if ("KHR".equalsIgnoreCase(currency)) {
            return "៛" + String.format("%,.0f", amount);
        }
        return amount + " " + currency;
    }
}