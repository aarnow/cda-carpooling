package com.cda.carpooling.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "users")
public class Role {

    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    public static final String ROLE_DRIVER = "ROLE_DRIVER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "label", nullable = false, unique = true, length = 50)
    private String label;

    @Column(name = "description")
    private String description;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    private Set<Person> people = new HashSet<>();
}