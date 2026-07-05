package com.ecommerce.application.api.dto.order;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Guest checkout: no authentication. An unregistered ({@code isRegistered=false}) AppUser is
 * created (or reused) for the supplied mobile, the address and cart are persisted under it, and the
 * order is placed. The guest can later complete the signup flow with the same mobile to claim the
 * account.
 */
@Getter
@Setter
public class GuestCheckoutRequestDto {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String mobile;

    @Email
    private String email;

    private String nationalId;

    @NotNull
    @Valid
    private AddressRequestDto address;

    @NotEmpty
    @Valid
    private List<GuestItemRequestDto> items;
}
