package com.seimad.patrimoine.entity.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Personne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personne")
    private Integer idPersonne;

    @Column(name = "nom", nullable = false, length = 200)
    private String nom;

    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "adresse", columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "email", columnDefinition = "TEXT")
    private String email;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "role", length = 50)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type_personne")
    private TypePersonne typePersonne;

    @PrePersist
    protected void onCreate() {
        if (date == null) {
            date = LocalDateTime.now();
        }
    }
}
