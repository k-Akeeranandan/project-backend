package com.klu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String date;

    // One Event -> Many Booths
    @OneToMany(mappedBy = "event",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private List<Booth> booths;
}