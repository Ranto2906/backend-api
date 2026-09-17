package com.seimad.patrimoine.repository.descente;

import com.seimad.patrimoine.entity.descente.DescenteTerrain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DescenteTerrainRepository extends JpaRepository<DescenteTerrain, UUID> {

    /**
     * Recherche multi-critères : référence, observation, personne (demandeur),
     * numéro dossier, numéro propriété, numéro lot, créateur.
     */
    @Query("SELECT d FROM DescenteTerrain d " +
           "LEFT JOIN d.personne p " +
           "LEFT JOIN d.dossierParcelle dp " +
           "LEFT JOIN dp.dossier dos " +
           "LEFT JOIN dp.propriete prop " +
           "LEFT JOIN dp.parcelle parc " +
           "LEFT JOIN d.utilisateurCreation uc " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "LOWER(d.reference) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(d.observation) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(d.statutConstat) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(d.validation) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(p.nom) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(dos.numeroDossier) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(prop.numero) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(parc.numeroLot) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(uc.nomComplet) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<DescenteTerrain> search(@Param("search") String search, Pageable pageable);

    /** Au moins une descente terrain rattachée aux lignes dossier_parcelle d'un dossier. */
    boolean existsByDossierParcelleDossierIdDossier(Integer idDossier);

    /**
     * Numéro max de référence pour un préfixe donné (ex : "DRT-2026-").
     * Utilisé pour générer la référence automatique DRT-AAAA-NNNN.
     */
    @Query("SELECT MAX(CAST(SUBSTRING(d.reference, :start) AS long)) FROM DescenteTerrain d " +
           "WHERE d.reference LIKE CONCAT(:prefix, '%')")
    Long maxNumeroReference(@Param("prefix") String prefix, @Param("start") int start);
}
