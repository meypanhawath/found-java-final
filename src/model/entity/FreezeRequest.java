// model/entity/FreezeRequest.java
package model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreezeRequest {
    private Integer id;
    private Integer accountId;
    private Integer requestedBy; // Customer ID
    private String status; // Pending, Approved, Rejected
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    // Additional fields for display (from joins)
    private String accountNo;
    private String accountName;
    private String customerName;

    // Constructor for creating new freeze request
    public FreezeRequest(Integer accountId, Integer requestedBy, String status) {
        this.accountId = accountId;
        this.requestedBy = requestedBy;
        this.status = status;
        this.requestedAt = LocalDateTime.now();
        this.processedAt = null;
    }

    /**
     * Check if request is pending
     */
    public boolean isPending() {
        return "Pending".equalsIgnoreCase(status);
    }

    /**
     * Check if request is approved
     */
    public boolean isApproved() {
        return "Approved".equalsIgnoreCase(status);
    }

    /**
     * Check if request is rejected
     */
    public boolean isRejected() {
        return "Rejected".equalsIgnoreCase(status);
    }

    /**
     * Get formatted requested date
     */
    public String getFormattedRequestedDate() {
        if (requestedAt == null) return "N/A";
        return requestedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Get formatted processed date
     */
    public String getFormattedProcessedDate() {
        if (processedAt == null) return "Not processed yet";
        return processedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Get status with emoji
     */
    public String getStatusDisplay() {
        return switch (status.toLowerCase()) {
            case "pending" -> "⏳ Pending";
            case "approved" -> "✅ Approved";
            case "rejected" -> "❌ Rejected";
            default -> "❓ " + status;
        };
    }

    /**
     * Get request summary
     */
    public String getRequestSummary() {
        return String.format("""
            Request ID: %d
            Account: %s (%s)
            Status: %s
            Requested: %s
            Processed: %s
            """,
                id,
                accountName != null ? accountName : "Unknown Account",
                accountNo != null ? formatAccountNo(accountNo) : "N/A",
                getStatusDisplay(),
                getFormattedRequestedDate(),
                getFormattedProcessedDate()
        );
    }

    /**
     * Format account number for display
     */
    private String formatAccountNo(String accountNo) {
        if (accountNo != null && accountNo.length() == 9) {
            return accountNo.substring(0, 3) + " " +
                    accountNo.substring(3, 6) + " " +
                    accountNo.substring(6, 9);
        }
        return accountNo;
    }

    // Enums for constants
    public enum Status {
        PENDING("Pending"),
        APPROVED("Approved"),
        REJECTED("Rejected");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}