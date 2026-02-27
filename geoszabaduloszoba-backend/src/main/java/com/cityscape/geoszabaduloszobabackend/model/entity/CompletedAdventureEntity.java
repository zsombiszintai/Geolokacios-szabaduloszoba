package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "completed_adventure")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CompletedAdventureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "adventure_id", nullable = false)
    private AdventureEntity adventure;

    @Column(nullable = false)
    private LocalDate completedAt = LocalDate.now();

    private Double distanceTravelled;

    private Integer durationSec;
    
}

