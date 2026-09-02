package com.ecommerce.application.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin creates a warehouse-staff account. The mobile doubles as the login username (like a normal
 * user); the account is created enabled + registered so the operator can sign in immediately.
 */
@Getter
@Setter
public class CreateStaffRequestDto {

    @NotBlank
    @Size(max = 255)
    private String firstName;

    @NotBlank
    @Size(max = 255)
    private String lastName;

    @NotEmpty
    @Pattern(regexp = "^09[0-9]{9}$", message = "Invalid mobile number format")
    private String mobile;

    @NotEmpty
    @Size(min = 6, max = 100)
    private String password;
}
