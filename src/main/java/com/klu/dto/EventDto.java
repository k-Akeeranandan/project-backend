package com.klu.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDto {

    private Long id;
    private String title;
    private String date;

    // Only booth IDs or simple details (no full nesting)
    private List<BoothDto> booths;
}