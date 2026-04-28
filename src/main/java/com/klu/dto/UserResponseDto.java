package com.klu.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private String role;

    /** PENDING until an admin approves; APPROVED users can sign in; REJECTED cannot. */
    private String accountStatus;

    private List<Long> appliedBoothIds; // 🔥 relationship

    /** Original resume filename if the user has uploaded one. */
    private String resumeOriginalFileName;
}