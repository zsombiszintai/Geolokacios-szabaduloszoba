package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "review")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "adventure_id", nullable = false)
    private AdventureEntity adventure;

    private Integer rating;

    @Column(length = 500)
    private String reviewText;

    private LocalDate reviewedAt = LocalDate.now();
}
