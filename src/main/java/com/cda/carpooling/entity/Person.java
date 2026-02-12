package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_person", indexes = {
        @Index(name = "idx_person_email", columnList = "email"),
        @Index(name = "idx_person_status", columnList = "id_person_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"roles", "vehicle", "bookings", "notifications", "profile"})
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_person")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    //region Relations
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_person_status", nullable = false)
    private PersonStatus status;

    @OneToOne(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private PersonProfile profile;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "person_role",
            joinColumns = @JoinColumn(name = "id_person"),
            inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private Vehicle vehicle;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Booking> bookings = new HashSet<>();

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Notification> notifications = new HashSet<>();
    //endregion

    //region Utils
    public void addRole(Role role) {
        this.roles.add(role);
        role.getPeople().add(this);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getPeople().remove(this);
    }
    //endregion
}