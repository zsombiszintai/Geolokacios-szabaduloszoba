package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_moderation_response")
@Getter
@Setter
@NoArgsConstructor
public class AiModerationResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adventure_id", nullable = false)
    private AdventureEntity adventure;

    private boolean isProfane;
    private boolean isSolvable;

    @Column(columnDefinition = "TEXT")
    private String profanityDetails;

    @Column(columnDefinition = "TEXT")
    private String solvabilityDetails;

    private boolean overallApproved;

    @Column(columnDefinition = "TEXT")
    private String reason;
}