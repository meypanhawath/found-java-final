// model/entity/Transaction.java
package model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private Integer id;
    private Integer senderId;           // Account ID (sender)
    private Integer receiverId;         // Account ID (receiver) - NULL for deposits/withdrawals
    private Integer transactionTypeId;  // Reference to transaction_types table
    private Integer billCategoryId;     // Reference to bill_categories (NULL if not bill payment)
    private BigDecimal amount;
    private String status;              // Pending, Success, Failed
    private String remark;
    private boolean isDeleted = false;
    private LocalDateTime createdAt;

    // Additional fields for display (from joins)
    private String transactionTypeName;
    private String senderAccountName;
    private String senderAccountNo;
    private String receiverAccountName;
    private String receiverAccountNo;
    private String billCategoryName;

    // Constructor for basic transaction (transfer/deposit/withdrawal)
    public Transaction(Integer senderId, Integer receiverId, Integer transactionTypeId,
                       BigDecimal amount, String status, String remark) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.transactionTypeId = transactionTypeId;
        this.billCategoryId = null; // Not a bill payment
        this.amount = amount;
        this.status = status;
        this.remark = remark;
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    // Constructor for bill payment (different parameter order to avoid conflict)
    public Transaction(Integer senderId, Integer transactionTypeId, Integer billCategoryId,
                       BigDecimal amount, String status, String remark, boolean isBillPayment) {
        this.senderId = senderId;
        this.receiverId = null; // No receiver for bill payments
        this.transactionTypeId = transactionTypeId;
        this.billCategoryId = billCategoryId;
        this.amount = amount;
        this.status = status;
        this.remark = remark;
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    // Static factory method for bill payments (cleaner approach)
    public static Transaction createBillPayment(Integer senderId, Integer transactionTypeId,
                                                Integer billCategoryId, BigDecimal amount,
                                                String status, String remark) {
        return new Transaction(senderId, transactionTypeId, billCategoryId, amount, status, remark, true);
    }

    // Static factory method for regular transactions
    public static Transaction createTransaction(Integer senderId, Integer receiverId,
                                                Integer transactionTypeId, BigDecimal amount,
                                                String status, String remark) {
        return new Transaction(senderId, receiverId, transactionTypeId, amount, status, remark);
    }

    /**
     * Get formatted amount with currency (we'll need account info for this)
     */
    public String getFormattedAmount(String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            return "$" + String.format("%,.2f", amount);
        } else if ("KHR".equalsIgnoreCase(currency)) {
            return "៛" + String.format("%,.0f", amount);
        }
        return amount + " " + currency;
    }

    /**
     * Get transaction description
     */
    public String getTransactionDescription() {
        if (billCategoryId != null && billCategoryName != null) {
            return "Bill Payment - " + billCategoryName;
        }

        if (receiverId == null) {
            return transactionTypeName; // Deposit or Withdrawal
        }

        return transactionTypeName + " to " +
                (receiverAccountName != null ? receiverAccountName : "Account " + receiverId);
    }

    /**
     * Get transaction status with emoji
     */
    public String getStatusDisplay() {
        return switch (status.toLowerCase()) {
            case "success" -> "✅ Success";
            case "pending" -> "⏳ Pending";
            case "failed" -> "❌ Failed";
            default -> status;
        };
    }

    /**
     * Check if transaction is successful
     */
    public boolean isSuccessful() {
        return "success".equalsIgnoreCase(status);
    }

    /**
     * Check if transaction is pending
     */
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    /**
     * Check if transaction failed
     */
    public boolean isFailed() {
        return "failed".equalsIgnoreCase(status);
    }
}