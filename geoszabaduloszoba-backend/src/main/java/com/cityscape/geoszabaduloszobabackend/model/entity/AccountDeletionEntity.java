package com.cityscape.geoszabaduloszobabackend.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Retained to reject already issued JWTs and retry interrupted deletions. */
@Entity
@Table(name = "account_deletion")
@Getter @Setter
public class AccountDeletionEntity {
    @Id
    private String subject;
    private boolean completed;
}
