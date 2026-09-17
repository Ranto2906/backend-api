package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.DossierParcelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DossierParcelleRepository extends JpaRepository<DossierParcelle, UUID> {

    List<DossierParcelle> findByDossier_IdDossierOrderByDateAjoutAsc(Integer idDossier);

    /**
     * Recherche de dossiers pour l'autocomplete du mobile.
     * Retourne les lignes dossier_parcelle avec les infos du dossier,
     * de la propriété, de la parcelle et du demandeur principal.
     */
    @Query("SELECT dp FROM DossierParcelle dp " +
           "LEFT JOIN FETCH dp.dossier dos " +
           "LEFT JOIN FETCH dp.propriete prop " +
           "LEFT JOIN FETCH dp.parcelle parc " +
           "LEFT JOIN FETCH dp.propriete.ville v " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "LOWER(dos.numeroDossier) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(prop.numero) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(parc.numeroLot) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(v.nomVille) LIKE LOWER(CONCAT('%',:search,'%')))")
    List<DossierParcelle> searchDossiers(@Param("search") String search);
}
