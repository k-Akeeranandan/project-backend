package com.klu.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoothDto {

    private Long id;
    private String companyName;
    private String description;

    private Long eventId;   

    private List<Long> applicantIds; 
}