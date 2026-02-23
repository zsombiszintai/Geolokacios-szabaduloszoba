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

    @Column(length = 20, unique = true, nullable = false)
    private String username;

    @Column(length = 50, unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private LocalDate registrationDate = LocalDate.now();

    @Column(length = 10, nullable = false)
    private String userRole = "USER";

    @Column(length = 100)
    private String profileDescription;

    @Column(length = 400)
    private String profilePictureUrl;

}



