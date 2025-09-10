package model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String username;
    private String password;
    private Role role;
    private Integer customerId;  // NULL for admins
    private Boolean isLocked;
    private Integer failedAttempts;
    private Boolean isDeleted;

    // Enum for role
    public enum Role {
        ADMIN, CUSTOMER
    }
}