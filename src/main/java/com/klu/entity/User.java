package com.klu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(unique = true)
    private String email;
    private String password;
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus = AccountStatus.PENDING;

    /** Stored file name under {@code file.upload-dir} (unique on disk). */
    private String resumeStoredFileName;
    /** Original filename from the user's upload (for download / display). */
    private String resumeOriginalFileName;
    private String resumeContentType;

    // Many Users -> Many Booths
    @ManyToMany(mappedBy = "applicants",
            fetch = FetchType.LAZY)
    private List<Booth> appliedBooths;
}