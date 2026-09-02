package com.ecommerce.application.api.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Enable or disable a warehouse-staff account (a disabled account can no longer sign in). */
@Getter
@Setter
public class UpdateStaffStatusRequestDto {

    @NotNull
    private Boolean enabled;
}
