package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter @Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String keycloakSub;

    private LocalDate registrationDate = LocalDate.now();

    @Column(length = 100)
    private String profileDescription;

    @Column(length = 400)
    private String profilePictureUrl;

    private Integer points = 0;

    @Transient
    private String avatarUrl;

}



