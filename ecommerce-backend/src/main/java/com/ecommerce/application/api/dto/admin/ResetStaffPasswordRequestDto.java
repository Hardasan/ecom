package com.ecommerce.application.api.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Admin sets a new password for a warehouse-staff account. */
@Getter
@Setter
public class ResetStaffPasswordRequestDto {

    @NotEmpty
    @Size(min = 6, max = 100)
    private String password;
}
