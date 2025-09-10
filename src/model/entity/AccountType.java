// model/entity/AccountType.java
package model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountType {
    private Integer id;
    private String type; // Saving, Checking, Fixed
    private boolean isDeleted = false;

    public AccountType(String type) {
        this.type = type;
        this.isDeleted = false;
    }
}