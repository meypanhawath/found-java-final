// controller/BankingController.java
package controller;

import model.entity.Account;
import model.entity.Customer;
import model.entity.Transaction;
import model.service.AccountService;
import model.service.TransactionService;
import model.service.CustomerService;
import view.BankingView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BankingController {
    private final BankingView bankingView;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CustomerService customerService;

    public BankingController() {
        this.bankingView = new BankingView();
        this.transactionService = new TransactionService();
        this.accountService = new AccountService();
        this.customerService = new CustomerService();
    }

    /**
     * Handle banking operations menu
     */
    public boolean handleBankingOperations(Customer customer) {
        if (customer == null) {
            bankingView.showErrorMessage("Customer information not found!");
            return false;
        }

        // Check if customer has accounts
        List<Account> customerAccounts = accountService.getCustomerAccounts(customer.getId());
        if (customerAccounts.isEmpty()) {
            bankingView.showInfoMessage("You don't have any accounts yet!");
            bankingView.showInfoMessage("Create an account first in Account Management.");
            bankingView.waitForEnter();
            return true;
        }

        while (true) {
            try {
                int choice = bankingView.showBankingMenu();

                switch (choice) {
                    case 0:
                        // Back to main menu
                        return true;
                    case 1:
                        handleDeposit(customer);
                        break;
                    case 2:
                        handleWithdrawal(customer);
                        break;
                    case 3:
                        handleTransfer(customer);
                        break;
                    case 4:
                        handleTransactionHistory(customer);
                        break;
                    case 5:
                        handleCheckBalance(customer);
                        break;
                    case 6:
                        handleDailyLimitsStatus(customer);
                        break;
                    default:
                        bankingView.showErrorMessage("Invalid choice! Please try again.");
                        bankingView.waitForEnter();
                }

            } catch (Exception e) {
                bankingView.showErrorMessage("An error occurred: " + e.getMessage());
                bankingView.waitForEnter();
            }
        }
    }

    /**
     * Handle deposit operation
     */
    private void handleDeposit(Customer customer) {
        try {
            // Get active accounts
            List<Account> activeAccounts = accountService.getActiveAccounts(customer.getId());

            if (activeAccounts.isEmpty()) {
                bankingView.showInfoMessage("No active accounts available for deposit!");
                bankingView.waitForEnter();
                return;
            }

            // Get deposit data
            BankingView.DepositData depositData = bankingView.getDepositData(activeAccounts);
            if (depositData == null) {
                return; // User cancelled
            }

            // Find the selected account
            Optional<Account> accountOpt = accountService.findAccountById(depositData.getAccountId());
            if (accountOpt.isEmpty()) {
                bankingView.showErrorMessage("Account not found!");
                bankingView.waitForEnter();
                return;
            }

            Account account = accountOpt.get();

            // Confirm transaction
            boolean confirmed = bankingView.confirmTransaction(
                    "DEPOSIT",
                    null, // No from account for deposits
                    account.getAccountDisplay(),
                    depositData.getAmount(),
                    account.getAccountCurrency(),
                    null // No exchange info for deposits
            );

            if (!confirmed) {
                bankingView.showInfoMessage("Deposit cancelled.");
                bankingView.waitForEnter();
                return;
            }

            // Validate PIN
            if (!validateCustomerPin(customer.getId(), depositData.getPin())) {
                bankingView.showErrorMessage("Invalid PIN!");
                bankingView.waitForEnter();
                return;
            }

            // Process deposit
            bankingView.showLoading("Processing deposit");
            boolean success = transactionService.deposit(
                    depositData.getAccountId(),
                    depositData.getAmount(),
                    depositData.getPin(),
                    depositData.getRemark()
            );

            if (success) {
                bankingView.showSuccessMessage("Deposit completed successfully!");
            } else {
                bankingView.showErrorMessage("Deposit failed. Please try again.");
            }

            bankingView.waitForEnter();

        } catch (Exception e) {
            bankingView.showErrorMessage("Error processing deposit: " + e.getMessage());
            bankingView.waitForEnter();
        }
    }

    /**
     * Handle withdrawal operation
     */
    private void handleWithdrawal(Customer customer) {
        try {
            // Get active accounts
            List<Account> activeAccounts = accountService.getActiveAccounts(customer.getId());

            if (activeAccounts.isEmpty()) {
                bankingView.showInfoMessage("No active accounts available for withdrawal!");
                bankingView.waitForEnter();
                return;
            }

            // Get withdrawal data
            BankingView.WithdrawalData withdrawalData = bankingView.getWithdrawalData(activeAccounts);
            if (withdrawalData == null) {
                return; // User cancelled
            }

            // Find the selected account
            Optional<Account> accountOpt = accountService.findAccountById(withdrawalData.getAccountId());
            if (accountOpt.isEmpty()) {
                bankingView.showErrorMessage("Account not found!");
                bankingView.waitForEnter();
                return;
            }

            Account account = accountOpt.get();

            // Check daily limits for Saving accounts
            String limitWarning = null;
            if ("Saving".equalsIgnoreCase(account.getAccountTypeName())) {
                BigDecimal remaining = transactionService.getRemainingDailyLimit(
                        account.getId(), account.getAccountCurrency());

                if (withdrawalData.getAmount().compareTo(remaining) > 0) {
                    bankingView.showErrorMessage("Daily limit exceeded!");
                    bankingView.showInfoMessage("Remaining daily limit: " +
                            formatCurrency(remaining, account.getAccountCurrency()));
                    bankingView.waitForEnter();
                    return;
                }

                limitWarning = "Daily limit remaining after withdrawal: " +
                        formatCurrency(remaining.subtract(withdrawalData.getAmount()), account.getAccountCurrency());
            }

            // Confirm transaction
            boolean confirmed = bankingView.confirmTransaction(
                    "WITHDRAWAL",
                    account.getAccountDisplay(),
                    null, // No to account for withdrawals
                    withdrawalData.getAmount(),
                    account.getAccountCurrency(),
                    limitWarning
            );

            if (!confirmed) {
                bankingView.showInfoMessage("Withdrawal cancelled.");
                bankingView.waitForEnter();
                return;
            }

            // Validate PIN
            if (!validateCustomerPin(customer.getId(), withdrawalData.getPin())) {
                bankingView.showErrorMessage("Invalid PIN!");
                bankingView.waitForEnter();
                return;
            }

            // Process withdrawal
            bankingView.showLoading("Processing withdrawal");
            boolean success = transactionService.withdraw(
                    withdrawalData.getAccountId(),
                    withdrawalData.getAmount(),
                    withdrawalData.getPin(),
                    withdrawalData.getRemark()
            );

            if (success) {
                bankingView.showSuccessMessage("Withdrawal completed successfully!");
            } else {
                bankingView.showErrorMessage("Withdrawal failed. Please try again.");
            }

            bankingView.waitForEnter();

        } catch (Exception e) {
            bankingView.showErrorMessage("Error processing withdrawal: " + e.getMessage());
            bankingView.waitForEnter();
        }
    }

    /**
     * Handle transfer operation
     */
    private void handleTransfer(Customer customer) {
        try {
            // Get active accounts for sending
            List<Account> activeAccounts = accountService.getActiveAccounts(customer.getId());

            if (activeAccounts.isEmpty()) {
                bankingView.showInfoMessage("No active accounts available for transfer!");
                bankingView.waitForEnter();
                return;
            }

            // Get all customer accounts (for internal transfers)
            List<Account> allCustomerAccounts = accountService.getCustomerAccounts(customer.getId());

            // Get transfer data
            BankingView.TransferData transferData = bankingView.getTransferData(activeAccounts, allCustomerAccounts);
            if (transferData == null) {
                return; // User cancelled
            }

            // Find sender account
            Optional<Account> fromAccountOpt = accountService.findAccountById(transferData.getFromAccountId());
            if (fromAccountOpt.isEmpty()) {
                bankingView.showErrorMessage("Sender account not found!");
                bankingView.waitForEnter();
                return;
            }

            Account fromAccount = fromAccountOpt.get();

            // Handle transfer based on type
            if (transferData.getTransferType() == BankingView.TransferType.INTERNAL) {
                handleInternalTransfer(customer, transferData, fromAccount);
            } else {
                handleExternalTransfer(customer, transferData, fromAccount);
            }

        } catch (Exception e) {
            bankingView.showErrorMessage("Error processing transfer: " + e.getMessage());
            bankingView.waitForEnter();
        }
    }

    /**
     * Handle internal transfer (to own accounts)
     */
    private void handleInternalTransfer(Customer customer, BankingView.TransferData transferData, Account fromAccount) {
        // Find receiver account
        Optional<Account> toAccountOpt = accountService.findAccountById(transferData.getToAccountId());
        if (toAccountOpt.isEmpty()) {
            bankingView.showErrorMessage("Receiver account not found!");
            bankingView.waitForEnter();
            return;
        }

        Account toAccount = toAccountOpt.get();

        // Check daily limits for Saving accounts
        String exchangeInfo = null;
        if ("Saving".equalsIgnoreCase(fromAccount.getAccountTypeName())) {
            BigDecimal remaining = transactionService.getRemainingDailyLimit(
                    fromAccount.getId(), fromAccount.getAccountCurrency());

            if (transferData.getAmount().compareTo(remaining) > 0) {
                bankingView.showErrorMessage("Daily limit exceeded!");
                bankingView.showInfoMessage("Remaining daily limit: " +
                        formatCurrency(remaining, fromAccount.getAccountCurrency()));
                bankingView.waitForEnter();
                return;
            }
        }

        // Check if currency conversion is needed
        if (!fromAccount.getAccountCurrency().equals(toAccount.getAccountCurrency())) {
            BigDecimal convertedAmount = transactionService.convertCurrency(
                    transferData.getAmount(), fromAccount.getAccountCurrency(), toAccount.getAccountCurrency());

            exchangeInfo = String.format("Currency conversion: %s → %s\nExchange rate: %s",
                    formatCurrency(transferData.getAmount(), fromAccount.getAccountCurrency()),
                    formatCurrency(convertedAmount, toAccount.getAccountCurrency()),
                    transactionService.getExchangeRate(fromAccount.getAccountCurrency(), toAccount.getAccountCurrency()));
        }

        // Confirm transaction
        boolean confirmed = bankingView.confirmTransaction(
                "INTERNAL TRANSFER",
                fromAccount.getAccountDisplay(),
                toAccount.getAccountDisplay(),
                transferData.getAmount(),
                fromAccount.getAccountCurrency(),
                exchangeInfo
        );

        if (!confirmed) {
            bankingView.showInfoMessage("Transfer cancelled.");
            bankingView.waitForEnter();
            return;
        }

        // Validate PIN
        if (!validateCustomerPin(customer.getId(), transferData.getPin())) {
            bankingView.showErrorMessage("Invalid PIN!");
            bankingView.waitForEnter();
            return;
        }

        // Process transfer
        bankingView.showLoading("Processing internal transfer");
        boolean success = transactionService.transfer(
                transferData.getFromAccountId(),
                transferData.getToAccountId(),
                transferData.getAmount(),
                transferData.getPin(),
                transferData.getRemark()
        );

        if (success) {
            bankingView.showSuccessMessage("Internal transfer completed successfully!");
        } else {
            bankingView.showErrorMessage("Transfer failed. Please try again.");
        }

        bankingView.waitForEnter();
    }

    /**
     * Handle external transfer (to other customers' accounts)
     */
    private void handleExternalTransfer(Customer customer, BankingView.TransferData transferData, Account fromAccount) {
        // Find receiver account by account number
        Optional<Account> toAccountOpt = accountService.findAccountByNumber(transferData.getExternalAccountNo());
        if (toAccountOpt.isEmpty()) {
            bankingView.showErrorMessage("Receiver account not found!");
            bankingView.showInfoMessage("Please verify the account number and try again.");
            bankingView.waitForEnter();
            return;
        }

        Account toAccount = toAccountOpt.get();

        // Check if trying to transfer to own account
        if (fromAccount.getCustomerId().equals(toAccount.getCustomerId())) {
            bankingView.showErrorMessage("Cannot transfer to your own account using external transfer!");
            bankingView.showInfoMessage("Use internal transfer for transfers to your own accounts.");
            bankingView.waitForEnter();
            return;
        }

        // Check if receiver account is active
        if (!toAccount.isActive()) {
            bankingView.showErrorMessage("Receiver account is not active!");
            bankingView.waitForEnter();
            return;
        }

        // Check daily limits for Saving accounts
        String exchangeInfo = null;
        if ("Saving".equalsIgnoreCase(fromAccount.getAccountTypeName())) {
            BigDecimal remaining = transactionService.getRemainingDailyLimit(
                    fromAccount.getId(), fromAccount.getAccountCurrency());

            if (transferData.getAmount().compareTo(remaining) > 0) {
                bankingView.showErrorMessage("Daily limit exceeded!");
                bankingView.showInfoMessage("Remaining daily limit: " +
                        formatCurrency(remaining, fromAccount.getAccountCurrency()));
                bankingView.waitForEnter();
                return;
            }
        }

        // Check if currency conversion is needed
        if (!fromAccount.getAccountCurrency().equals(toAccount.getAccountCurrency())) {
            BigDecimal convertedAmount = transactionService.convertCurrency(
                    transferData.getAmount(), fromAccount.getAccountCurrency(), toAccount.getAccountCurrency());

            exchangeInfo = String.format("Currency conversion: %s → %s\nExchange rate: %s",
                    formatCurrency(transferData.getAmount(), fromAccount.getAccountCurrency()),
                    formatCurrency(convertedAmount, toAccount.getAccountCurrency()),
                    transactionService.getExchangeRate(fromAccount.getAccountCurrency(), toAccount.getAccountCurrency()));
        }

        // Show receiver account info (masked for privacy)
        String maskedReceiverInfo = toAccount.getAccountName() + " - " +
                maskAccountNumber(toAccount.getFormattedAccountNo());

        // Confirm transaction
        boolean confirmed = bankingView.confirmTransaction(
                "EXTERNAL TRANSFER",
                fromAccount.getAccountDisplay(),
                maskedReceiverInfo,
                transferData.getAmount(),
                fromAccount.getAccountCurrency(),
                exchangeInfo
        );

        if (!confirmed) {
            bankingView.showInfoMessage("Transfer cancelled.");
            bankingView.waitForEnter();
            return;
        }

        // Validate PIN
        if (!validateCustomerPin(customer.getId(), transferData.getPin())) {
            bankingView.showErrorMessage("Invalid PIN!");
            bankingView.waitForEnter();
            return;
        }

        // Process transfer
        bankingView.showLoading("Processing external transfer");
        boolean success = transactionService.transfer(
                transferData.getFromAccountId(),
                toAccount.getId(),
                transferData.getAmount(),
                transferData.getPin(),
                transferData.getRemark()
        );

        if (success) {
            bankingView.showSuccessMessage("External transfer completed successfully!");
        } else {
            bankingView.showErrorMessage("Transfer failed. Please try again.");
        }

        bankingView.waitForEnter();
    }

    /**
     * Handle transaction history
     */
    private void handleTransactionHistory(Customer customer) {
        try {
            bankingView.showLoading("Loading transaction history");
            List<Transaction> transactions = transactionService.getCustomerTransactionHistory(customer.getId());

            bankingView.displayTransactionHistory(transactions);

            if (!transactions.isEmpty()) {
                System.out.println("\nTotal Transactions: " + transactions.size());

                // Show transaction summary
                long successfulTransactions = transactions.stream()
                        .filter(Transaction::isSuccessful)
                        .count();

                System.out.println("Successful: " + successfulTransactions);
                System.out.println("Failed/Pending: " + (transactions.size() - successfulTransactions));
            }

            bankingView.waitForEnter();

        } catch (Exception e) {
            bankingView.showErrorMessage("Error loading transaction history: " + e.getMessage());
            bankingView.waitForEnter();
        }
    }

    /**
     * Handle check balance
     */
    private void handleCheckBalance(Customer customer) {
        try {
            List<Account> activeAccounts = accountService.getActiveAccounts(customer.getId());

            if (activeAccounts.isEmpty()) {
                bankingView.showInfoMessage("No active accounts found!");
                bankingView.waitForEnter();
                return;
            }

            if (activeAccounts.size() == 1) {
                // Only one account, show it directly
                Account account = activeAccounts.get(0);
                BigDecimal remainingLimit = null;

                if ("Saving".equalsIgnoreCase(account.getAccountTypeName())) {
                    remainingLimit = transactionService.getRemainingDailyLimit(
                            account.getId(), account.getAccountCurrency());
                }

                bankingView.displayAccountBalance(account, remainingLimit);
            } else {
                // Multiple accounts, let user choose
                System.out.println("\nSelect account to check balance:");
                int accountIndex = selectAccountFromList(activeAccounts);
                if (accountIndex == -1) {
                    return;
                }

                Account account = activeAccounts.get(accountIndex);
                BigDecimal remainingLimit = null;

                if ("Saving".equalsIgnoreCase(account.getAccountTypeName())) {
                    remainingLimit = transactionService.getRemainingDailyLimit(
                            account.getId(), account.getAccountCurrency());
                }

                bankingView.displayAccountBalance(account, remainingLimit);
            }

            bankingView.waitForEnter();

        } catch (Exception e) {
            bankingView.showErrorMessage("Error checking balance: " + e.getMessage());
            bankingView.waitForEnter();
        }
    }

    /**
     * Handle daily limits status
     */
    private void handleDailyLimitsStatus(Customer customer) {
        try {
            bankingView.showLoading("Loading daily limits status");
            List<Account> accounts = accountService.getActiveAccounts(customer.getId());

            Map<Integer, BigDecimal> remainingLimits = new HashMap<>();

            for (Account account : accounts) {
                if ("Saving".equalsIgnoreCase(account.getAccountTypeName())) {
                    BigDecimal remaining = transactionService.getRemainingDailyLimit(
                            account.getId(), account.getAccountCurrency());
                    remainingLimits.put(account.getId(), remaining);
                }
            }

            bankingView.displayDailyLimitsStatus(accounts, remainingLimits);

            // Show exchange rates
            System.out.println("\n💱 Current Exchange Rates:");
            System.out.println("   • " + transactionService.getExchangeRate("USD", "KHR"));
            System.out.println("   • " + transactionService.getExchangeRate("KHR", "USD"));

            bankingView.waitForEnter();

        } catch (Exception e) {
            bankingView.showErrorMessage("Error loading daily limits: " + e.getMessage());
            bankingView.waitForEnter();
        }
    }

    /**
     * Validate customer PIN
     */
    private boolean validateCustomerPin(Integer customerId, String pin) {
        try {
            Optional<Customer> customerOpt = customerService.findCustomerById(customerId);
            if (customerOpt.isEmpty()) {
                return false;
            }

            Customer customer = customerOpt.get();
            return customer.getPin().equals(pin);

        } catch (Exception e) {
            System.err.println("Error validating PIN: " + e.getMessage());
            return false;
        }
    }

    /**
     * Select account from list helper method
     */
    private int selectAccountFromList(List<Account> accounts) {
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            System.out.printf("%d. %s - %s\n", i + 1, account.getAccountDisplay(), account.getFormattedBalance());
        }

        System.out.print("\nSelect account (1-" + accounts.size() + "): ");

        try {
            int selection = Integer.parseInt(System.console() != null ?
                    System.console().readLine().trim() :
                    new java.util.Scanner(System.in).nextLine().trim());

            if (selection >= 1 && selection <= accounts.size()) {
                return selection - 1;
            }
        } catch (NumberFormatException e) {
            // Invalid input
        }

        bankingView.showErrorMessage("Invalid account selection!");
        return -1;
    }

    /**
     * Format currency helper method
     */
    private String formatCurrency(BigDecimal amount, String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            return "$" + String.format("%,.2f", amount);
        } else if ("KHR".equalsIgnoreCase(currency)) {
            return "៛" + String.format("%,.0f", amount);
        }
        return amount + " " + currency;
    }

    /**
     * Mask account number for privacy
     */
    private String maskAccountNumber(String formattedAccountNo) {
        if (formattedAccountNo == null || formattedAccountNo.length() < 11) {
            return "*** *** ***";
        }
        // Show only last 3 digits: "*** *** 123"
        return "*** *** " + formattedAccountNo.substring(8);
    }

    /**
     * Get transaction service for external access
     */
    public TransactionService getTransactionService() {
        return transactionService;
    }
}