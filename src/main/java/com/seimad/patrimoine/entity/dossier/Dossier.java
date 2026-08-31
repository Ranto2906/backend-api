package com.seimad.patrimoine.entity.dossier;

import com.seimad.patrimoine.entity.referentiel.Ville;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dossier")
    private Integer idDossier;

    @Column(name = "numero_dossier", nullable = false, length = 30)
    private String numeroDossier;

    @Column(name = "date_demande")
    private LocalDate dateDemande;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "transmission", length = 50)
    private String transmission;

    @Column(name = "resultat_etude", length = 255)
    private String resultatEtude;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    @Builder.Default
    private LocalDateTime dateModification = LocalDateTime.now();

    @Column(name = "date_validation")
    private LocalDate dateValidation;

    @Column(name = "montant_total", precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_statut_dossier")
    private StatutDossier statutDossier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ville", nullable = false)
    private Ville ville;
}
