package com.ecommerce.application.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/** A warehouse-staff account as shown in the admin staff list (never exposes the password). */
@Getter
@Setter
@AllArgsConstructor
public class StaffResponseDto {

    private Long id;

    private String firstName;

    private String lastName;

    private String mobile;

    private boolean enabled;

    private Date createdAt;
}
