package com.klu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "booths")
public class Booth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;
    private String description;

    // Many Booths -> One Event
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id")
    private Event event;

    // One Booth -> Many Users (Applicants)
    @ManyToMany
    @JoinTable(
            name = "applications",
            joinColumns = @JoinColumn(name = "booth_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> applicants;
}