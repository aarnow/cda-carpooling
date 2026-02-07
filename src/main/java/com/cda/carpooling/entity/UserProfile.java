package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant le profil d'un utilisateur.
 */
@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user_profile")
    private Long id;

    @Column(name = "lastname", nullable = false, length = 50)
    private String lastname;

    @Column(name = "firstname", nullable = false, length = 50)
    private String firstname;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "phone", length = 10)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "id_user", nullable = false)
    private User user;
}