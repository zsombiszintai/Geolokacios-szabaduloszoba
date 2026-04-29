package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name="adventure_list")
public class ListEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(length = 50, unique = true, nullable = false)
   private String title;

   @Column(length = 500)
   private String description;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "creator_id", nullable = false)
   private UserEntity creator;

    @ManyToMany
    @JoinTable(
            name = "list_adventures",
            joinColumns = @JoinColumn(name = "list_id"),
            inverseJoinColumns = @JoinColumn(name = "adventure_id")
    )
    private List<AdventureEntity> adventures = new ArrayList<>();
}
