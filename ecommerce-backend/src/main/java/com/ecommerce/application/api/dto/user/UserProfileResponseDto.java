package com.ecommerce.application.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponseDto {

    private String firstName;
    private String lastName;
    private String mobile;
}
