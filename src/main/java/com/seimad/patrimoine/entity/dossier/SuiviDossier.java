package com.seimad.patrimoine.entity.dossier;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "suivi_dossier", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_dossier", "id_etape"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuiviDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suivi")
    private Integer idSuivi;

    @Column(name = "date_realisation")
    private LocalDate dateRealisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_statut_couleur")
    private StatutCouleur statutCouleur;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_creation")
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    @Builder.Default
    private LocalDateTime dateModification = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etape", nullable = false)
    private Etape etape;
}
