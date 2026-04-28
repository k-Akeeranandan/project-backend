package com.klu.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDto {

    @NotBlank(message="Name required")
    private String name;

    @Email(message="Invalid email")
    private String email;

    @NotBlank(message="Password required")
    private String password;
}