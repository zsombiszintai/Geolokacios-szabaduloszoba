package com.cityscape.geoszabaduloszobabackend.model.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Getter
@Table(name = "user_adventure_statistics")
public class UserAdventureStatistics {
    @Id
    @Column(name = "user_id")
    private Long id;

    private String keycloakSub;
    private String username;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "profile_description")
    private String profileDescription;

    private Long completedCount;
    private Long abandonedCount;
    private Long ownedCount;
    private Long ratedCount;
    private Long reviewsCount;
}
