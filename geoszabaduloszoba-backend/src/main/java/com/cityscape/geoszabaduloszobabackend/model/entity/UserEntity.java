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

    private String password;

    private LocalDate registrationDate = LocalDate.now();

    private String userRole = "USER";

    @Column(length = 100)
    private String profileDescription;

    @Column(length = 400)
    private String profilePictureUrl;

}



