package com.cityscape.geoszabaduloszobabackend.model.entity;

import com.cityscape.geoszabaduloszobabackend.model.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "adventure")
@Data
public class AdventureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private Difficulty difficulty;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creator;

    @Column(length = 20, nullable = false)
    private String status = "PENDING";

    @Column(nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    @Column(nullable = false)
    private Double totalDistance = 0.0;

    // Existing schemas require a value in this column when inserting an adventure.
    @Column(name = "has_starting_point", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean legacyStartingPoint;

    @Column(name = "average_rating", insertable = false, updatable = false)
    private Double averageRating;

    @Column(name = "average_time_in_seconds", insertable = false, updatable = false)
    private Integer averageTimeInSeconds;
}
