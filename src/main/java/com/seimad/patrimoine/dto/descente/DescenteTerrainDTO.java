package com.seimad.patrimoine.dto.descente;

import com.seimad.patrimoine.dto.dossier.DossierDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DescenteTerrainDTO {

    private UUID idDescente;
    private String reference;
    private LocalDate dateDescente;
    private String statutConstat;
    private String observation;
    private String mode;
    private String validation;
    private LocalDateTime dateValidation;
    private Boolean synchronise;
    private LocalDateTime createdAt;
    private LocalDateTime dateModification;

    // Utilisateur création
    private Integer idUtilisateurCreation;
    private String nomUtilisateurCreation;

    // Utilisateur validation
    private Integer idUtilisateurValidation;
    private String nomUtilisateurValidation;

    // Personne (demandeur)
    private Integer idPersonne;
    private String nomPersonne;
    private String contactPersonne;

    // DossierParcelle → Dossier
    private UUID idDossierParcelle;
    private Integer idDossier;
    private String numeroDossier;

    // DossierParcelle → Propriete
    private Integer idPropriete;
    private String numeroPropriete;

    // DossierParcelle → Parcelle
    private UUID idParcelle;
    private String numeroLot;

    // Superficie
    private BigDecimal superficieM2;

    // Villes (depuis le dossier → ville)
    private Integer idVille;
    private String nomVille;

    /**
     * Dossier complet rattaché (statut, type, dates, montant, demandeurs,
     * parcelles, suivi) — peuplé uniquement par /api/descentes-terrain/{id}.
     */
    private DossierDTO dossier;
}
