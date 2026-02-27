package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "station")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
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

    @Column(length = 100)
    private String riddleText;
}
