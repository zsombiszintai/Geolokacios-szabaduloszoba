package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "abandoned_adventure")
@Data
public class AbandonedAdventureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "adventure_id", nullable = false)
    private AdventureEntity adventure;

    private LocalDateTime startedAt = LocalDateTime.now();

    private Long lastStationId;

    private Double distanceTravelled = 0.0;
    private Integer elapsedSec = 0;

    private boolean isCompleted = false;
}

