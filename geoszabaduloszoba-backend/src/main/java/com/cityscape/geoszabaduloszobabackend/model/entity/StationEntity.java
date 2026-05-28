package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "station")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "adventure_id", nullable = false)
    private AdventureEntity adventure;

    @Column(nullable = false)
    private Integer seqNumber;

    private Double latitude;

    private Double longitude;

    /**
     * - riddle
     * - explanation
     * - hints
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    private boolean isLastStation;
}
