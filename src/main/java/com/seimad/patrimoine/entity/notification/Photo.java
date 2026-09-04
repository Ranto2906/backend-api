package com.seimad.patrimoine.entity.notification;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Photo polymorphe : rattachée à notification, suivi, avertissement,
 * signalement ou descente_terrain via (entite_type, entite_id).
 */
@Entity
@Table(name = "photo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {

    @Id
    @Column(name = "id_photo")
    private UUID idPhoto;

    @Column(name = "entite_type", nullable = false, length = 30)
    private String entiteType;

    @Column(name = "entite_id", nullable = false)
    private UUID entiteId;

    @Column(name = "type_photo", length = 50)
    private String typePhoto;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "chemin_fichier", columnDefinition = "TEXT")
    private String cheminFichier;

    @Column(name = "date_prise")
    private LocalDate datePrise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @Column(name = "date_creation")
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
