// view/BankingView.java
package view;

import model.entity.Account;
import model.entity.Transaction;
import util.AccountUtil;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class BankingView {
    private final Scanner scanner;

    public BankingView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Show banking operations menu
     */
    public int showBankingMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           BANKING OPERATIONS");
        System.out.println("=".repeat(50));
        System.out.println("1. Deposit Money");
        System.out.println("2. Withdraw Money");
        System.out.println("3. Transfer Money");
        System.out.println("4. Transaction History");
        System.out.println("5. Check Balance");
        System.out.println("6. Daily Limits Status");
        System.out.println("0. Back to Main Menu");
        System.out.print("Choose an option: ");

        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Get deposit transaction data
     */
    public DepositData getDepositData(List<Account> accounts) {
        if (accounts.isEmpty()) {
            showErrorMessage("No accounts available for deposit!");
            return null;
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("               DEPOSIT MONEY");
        System.out.println("=".repeat(50));

        // Select account
        int accountIndex = selectAccount(accounts, "Select account to deposit into:");
        if (accountIndex == -1) return null;

        Account selectedAccount = accounts.get(accountIndex);

        // Get amount
        BigDecimal amount = getAmountInput("deposit", selectedAccount.getAccountCurrency());
        if (amount == null) return null;

        // Get PIN
        String pin = getPinInput();
        if (pin == null) return null;

        // Get remark (optional)
        System.out.print("Transaction remark (optional): ");
        String remark = scanner.nextLine().trim();
        if (remark.isEmpty()) remark = null;

        return new DepositData(selectedAccount.getId(), amount, pin, remark);
    }

    /**
     * Get withdrawal transaction data
     */
    public WithdrawalData getWithdrawalData(List<Account> accounts) {
        if (accounts.isEmpty()) {
            showErrorMessage("No accounts available for withdrawal!");
            return null;
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("              WITHDRAW MONEY");
        System.out.println("=".repeat(50));

        // Select account
        int accountIndex = selectAccount(accounts, "Select account to withdraw from:");
        if (accountIndex == -1) return null;

        Account selectedAccount = accounts.get(accountIndex);

        // Show current balance
        System.out.println("Current Balance: " + selectedAccount.getFormattedBalance());

        // Check if Fixed account is matured
        if (selectedAccount.getMaturityDate() != null && !selectedAccount.isMatured()) {
            showErrorMessage("Fixed account withdrawals not allowed until maturity: " + selectedAccount.getMaturityDate());
            return null;
        }

        // Get amount
        BigDecimal amount = getAmountInput("withdraw", selectedAccount.getAccountCurrency());
        if (amount == null) return null;

        // Validate amount against balance
        if (amount.compareTo(selectedAccount.getBalance()) > 0) {
            showErrorMessage("Insufficient balance! Available: " + selectedAccount.getFormattedBalance());
            return null;
        }

        // Get PIN
        String pin = getPinInput();
        if (pin == null) return null;

        // Get remark (optional)
        System.out.print("Transaction remark (optional): ");
        String remark = scanner.nextLine().trim();
        if (remark.isEmpty()) remark = null;

        return new WithdrawalData(selectedAccount.getId(), amount, pin, remark);
    }

    /**
     * Get transfer transaction data
     */
    public TransferData getTransferData(List<Account> fromAccounts, List<Account> allCustomerAccounts) {
        if (fromAccounts.isEmpty()) {
            showErrorMessage("No accounts available for transfer!");
            return null;
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("              TRANSFER MONEY");
        System.out.println("=".repeat(50));

        // Select sender account
        int fromAccountIndex = selectAccount(fromAccounts, "Select account to transfer FROM:");
        if (fromAccountIndex == -1) return null;

        Account fromAccount = fromAccounts.get(fromAccountIndex);

        // Show current balance
        System.out.println("Current Balance: " + fromAccount.getFormattedBalance());

        // Check if Fixed account is matured
        if (fromAccount.getMaturityDate() != null && !fromAccount.isMatured()) {
            showErrorMessage("Fixed account transfers not allowed until maturity: " + fromAccount.getMaturityDate());
            return null;
        }

        // Choose transfer type
        TransferType transferType = getTransferType();
        if (transferType == null) return null;

        Account toAccount = null;

        if (transferType == TransferType.INTERNAL) {
            // Transfer to own account
            List<Account> otherAccounts = allCustomerAccounts.stream()
                    .filter(acc -> !acc.getId().equals(fromAccount.getId()))
                    .filter(Account::isActive)
                    .toList();

            if (otherAccounts.isEmpty()) {
                showErrorMessage("No other accounts available for internal transfer!");
                return null;
            }

            int toAccountIndex = selectAccount(otherAccounts, "Select account to transfer TO:");
            if (toAccountIndex == -1) return null;
            toAccount = otherAccounts.get(toAccountIndex);

        } else {
            // Transfer to external account
            String externalAccountNo = getExternalAccountNumber();
            if (externalAccountNo == null) return null;
            // Note: We'll handle external account validation in the service
        }

        // Get amount
        BigDecimal amount = getAmountInput("transfer", fromAccount.getAccountCurrency());
        if (amount == null) return null;

        // Validate amount against balance
        if (amount.compareTo(fromAccount.getBalance()) > 0) {
            showErrorMessage("Insufficient balance! Available: " + fromAccount.getFormattedBalance());
            return null;
        }

        // Get PIN
        String pin = getPinInput();
        if (pin == null) return null;

        // Get remark (optional)
        System.out.print("Transfer remark (optional): ");
        String remark = scanner.nextLine().trim();
        if (remark.isEmpty()) remark = null;

        if (transferType == TransferType.INTERNAL) {
            return new TransferData(fromAccount.getId(), toAccount.getId(), amount, pin, remark, transferType);
        } else {
            // For external transfers, we'll use account number
            String externalAccountNo = new String();
            return new TransferData(fromAccount.getId(), externalAccountNo, amount, pin, remark, transferType);
        }
    }

    /**
     * Select account from list
     */
    private int selectAccount(List<Account> accounts, String prompt) {
        System.out.println("\n" + prompt);
        System.out.println("-".repeat(50));

        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            System.out.printf("%d. %s - %s\n", i + 1, account.getAccountDisplay(), account.getFormattedBalance());
            if (account.getMaturityDate() != null && !account.isMatured()) {
                System.out.println("   ⚠️ Withdrawals restricted until: " + account.getMaturityDate());
            }
        }

        System.out.print("\nSelect account (1-" + accounts.size() + "): ");

        try {
            int selection = Integer.parseInt(scanner.nextLine().trim());
            if (selection >= 1 && selection <= accounts.size()) {
                return selection - 1;
            }
        } catch (NumberFormatException e) {
            // Invalid input
        }

        showErrorMessage("Invalid account selection!");
        return -1;
    }

    /**
     * Get transfer type
     */
    private TransferType getTransferType() {
        System.out.println("\nTransfer Options:");
        System.out.println("1. Transfer to My Other Account");
        System.out.println("2. Transfer to External Account");
        System.out.print("Choose transfer type (1-2): ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice == 1) {
                return TransferType.INTERNAL;
            } else if (choice == 2) {
                return TransferType.EXTERNAL;
            }
        } catch (NumberFormatException e) {
            // Invalid input
        }

        showErrorMessage("Invalid transfer type selection!");
        return null;
    }

    /**
     * Get external account number
     */
    private String getExternalAccountNumber() {
        System.out.print("Enter recipient account number (xxx xxx xxx or xxxxxxxxx): ");
        String accountNo = scanner.nextLine().trim();

        if (accountNo.isEmpty()) {
            showErrorMessage("Account number cannot be empty!");
            return null;
        }

        // Clean account number (remove spaces)
        accountNo = accountNo.replaceAll("\\s+", "");

        // Validate format
        if (!accountNo.matches("\\d{9}")) {
            showErrorMessage("Invalid account number format! Must be 9 digits.");
            return null;
        }

        return accountNo;
    }

    /**
     * Get amount input with validation
     */
    private BigDecimal getAmountInput(String operation, String currency) {
        String currencySymbol = AccountUtil.getCurrencySymbol(currency);

        while (true) {
            System.out.print("Enter amount to " + operation + ": " + currencySymbol);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                showErrorMessage("Amount cannot be empty!");
                continue;
            }

            try {
                BigDecimal amount = new BigDecimal(input);

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showErrorMessage("Amount must be greater than 0!");
                    continue;
                }

                // For KHR, ensure it's a whole number
                if ("KHR".equals(currency) && amount.scale() > 0) {
                    showErrorMessage("KHR amount cannot have decimal places!");
                    continue;
                }

                return amount;

            } catch (NumberFormatException e) {
                showErrorMessage("Invalid amount format!");
            }
        }
    }

    /**
     * Get PIN input
     */
    private String getPinInput() {
        System.out.print("Enter your 6-digit banking PIN: ");
        String pin = scanner.nextLine().trim();

        if (pin.isEmpty()) {
            showErrorMessage("PIN cannot be empty!");
            return null;
        }

        if (!pin.matches("\\d{6}")) {
            showErrorMessage("PIN must be exactly 6 digits!");
            return null;
        }

        return pin;
    }

    /**
     * Display transaction confirmation
     */
    public boolean confirmTransaction(String transactionType, String fromAccount, String toAccount,
                                      BigDecimal amount, String currency, String exchangeInfo) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("         CONFIRM " + transactionType.toUpperCase());
        System.out.println("=".repeat(50));

        if (fromAccount != null) {
            System.out.println("From Account: " + fromAccount);
        }
        if (toAccount != null) {
            System.out.println("To Account: " + toAccount);
        }

        System.out.println("Amount: " + AccountUtil.formatCurrency(amount, currency));

        if (exchangeInfo != null) {
            System.out.println(exchangeInfo);
        }

        System.out.println("=".repeat(50));
        System.out.print("Confirm transaction? (y/n): ");

        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }

    /**
     * Display transaction history
     */
    public void displayTransactionHistory(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("\n📝 No transactions found.");
            return;
        }

        System.out.println("\n" + "=".repeat(75));
        System.out.println("                           TRANSACTION HISTORY");
        System.out.println("=".repeat(75));

        for (Transaction transaction : transactions) {
            System.out.println("Date: " + transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
            System.out.println("Type: " + transaction.getTransactionTypeName());
            System.out.println("Status: " + transaction.getStatusDisplay());
            System.out.println("Amount: " + formatTransactionAmount(transaction));

            if (transaction.getSenderAccountNo() != null) {
                System.out.println("From: " + transaction.getSenderAccountName() + " - " +
                        AccountUtil.formatAccountNumber(transaction.getSenderAccountNo()));
            }

            if (transaction.getReceiverAccountNo() != null) {
                System.out.println("To: " + transaction.getReceiverAccountName() + " - " +
                        AccountUtil.formatAccountNumber(transaction.getReceiverAccountNo()));
            }

            if (transaction.getRemark() != null) {
                System.out.println("Remark: " + transaction.getRemark());
            }

            System.out.println("-".repeat(75));
        }
    }

    /**
     * Display account balance
     */
    public void displayAccountBalance(Account account, BigDecimal remainingDailyLimit) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("              ACCOUNT BALANCE");
        System.out.println("=".repeat(50));
        System.out.println("Account: " + account.getAccountDisplay());
        System.out.println("Current Balance: " + account.getFormattedBalance());
        System.out.println("Status: " + account.getStatus());

        if (remainingDailyLimit != null && "Saving".equalsIgnoreCase(account.getAccountTypeName())) {
            System.out.println("Daily Limit Remaining: " +
                    AccountUtil.formatCurrency(remainingDailyLimit, account.getAccountCurrency()));
        }

        System.out.println("=".repeat(50));
    }

    /**
     * Display daily limits status
     */
    public void displayDailyLimitsStatus(List<Account> accounts, java.util.Map<Integer, BigDecimal> remainingLimits) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                 DAILY LIMITS STATUS");
        System.out.println("=".repeat(60));

        for (Account account : accounts) {
            System.out.println("Account: " + account.getAccountDisplay());

            if ("Saving".equalsIgnoreCase(account.getAccountTypeName())) {
                BigDecimal remaining = remainingLimits.get(account.getId());
                if (remaining != null) {
                    System.out.println("Daily Limit Remaining: " +
                            AccountUtil.formatCurrency(remaining, account.getAccountCurrency()));
                }
            } else {
                System.out.println("Daily Limit: No limit");
            }

            System.out.println("-".repeat(40));
        }
    }

    /**
     * Format transaction amount for display
     */
    private String formatTransactionAmount(Transaction transaction) {
        // This is a simplified version - in a real system, we'd need currency info
        return "$" + String.format("%,.2f", transaction.getAmount());
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

    // ========== DATA CLASSES ==========

    public enum TransferType {
        INTERNAL, EXTERNAL
    }

    public static class DepositData {
        private final Integer accountId;
        private final BigDecimal amount;
        private final String pin;
        private final String remark;

        public DepositData(Integer accountId, BigDecimal amount, String pin, String remark) {
            this.accountId = accountId;
            this.amount = amount;
            this.pin = pin;
            this.remark = remark;
        }

        // Getters
        public Integer getAccountId() { return accountId; }
        public BigDecimal getAmount() { return amount; }
        public String getPin() { return pin; }
        public String getRemark() { return remark; }
    }

    public static class WithdrawalData {
        private final Integer accountId;
        private final BigDecimal amount;
        private final String pin;
        private final String remark;

        public WithdrawalData(Integer accountId, BigDecimal amount, String pin, String remark) {
            this.accountId = accountId;
            this.amount = amount;
            this.pin = pin;
            this.remark = remark;
        }

        // Getters
        public Integer getAccountId() { return accountId; }
        public BigDecimal getAmount() { return amount; }
        public String getPin() { return pin; }
        public String getRemark() { return remark; }
    }

    public static class TransferData {
        private final Integer fromAccountId;
        private final Integer toAccountId;
        private final String externalAccountNo;
        private final BigDecimal amount;
        private final String pin;
        private final String remark;
        private final TransferType transferType;

        // Constructor for internal transfers
        public TransferData(Integer fromAccountId, Integer toAccountId, BigDecimal amount,
                            String pin, String remark, TransferType transferType) {
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.externalAccountNo = null;
            this.amount = amount;
            this.pin = pin;
            this.remark = remark;
            this.transferType = transferType;
        }

        // Constructor for external transfers
        public TransferData(Integer fromAccountId, String externalAccountNo, BigDecimal amount,
                            String pin, String remark, TransferType transferType) {
            this.fromAccountId = fromAccountId;
            this.toAccountId = null;
            this.externalAccountNo = externalAccountNo;
            this.amount = amount;
            this.pin = pin;
            this.remark = remark;
            this.transferType = transferType;
        }

        // Getters
        public Integer getFromAccountId() { return fromAccountId; }
        public Integer getToAccountId() { return toAccountId; }
        public String getExternalAccountNo() { return externalAccountNo; }
        public BigDecimal getAmount() { return amount; }
        public String getPin() { return pin; }
        public String getRemark() { return remark; }
        public TransferType getTransferType() { return transferType; }
    }
}