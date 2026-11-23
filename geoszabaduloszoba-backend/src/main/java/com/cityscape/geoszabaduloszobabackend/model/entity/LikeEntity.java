package com.cityscape.geoszabaduloszobabackend.model.entity;

import com.cityscape.geoszabaduloszobabackend.model.enums.LikeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter @Setter
@Table(name = "user_likes", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_likes_target", columnNames = {"liker_id", "liked_id", "type"}))
public class LikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liker_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity liker;

    @Column(name = "liked_id", nullable = false, updatable = false)
    private Long likedId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private LikeType type;

    @Column(name = "like_date", nullable = false, updatable = false)
    private Instant likeDate;
}
