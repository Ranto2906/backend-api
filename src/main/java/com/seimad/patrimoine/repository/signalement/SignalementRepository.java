package com.seimad.patrimoine.repository.signalement;

import com.seimad.patrimoine.entity.signalement.Signalement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement, UUID> {

    @Query("SELECT s FROM Signalement s WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.reference) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(CAST(s.typeSignalement.libelle AS string)) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(CAST(s.statutSignalement.libelle AS string)) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(CAST(s.ville.nomVille AS string)) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(CAST(s.titreFoncier.numero AS string)) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(CAST(s.parcelle.numeroLot AS string)) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(CAST(s.utilisateurCreation.nomComplet AS string)) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Signalement> search(@Param("search") String search, Pageable pageable);

    List<Signalement> findByVilleIdVille(Integer idVille);

    List<Signalement> findByStatutSignalementIdStatutSignalement(Integer idStatutSignalement);

    List<Signalement> findByTypeSignalementIdTypeSignalement(Integer idTypeSignalement);

    List<Signalement> findByTitreFoncierIdTitreFoncier(UUID idTitreFoncier);

    List<Signalement> findByParcelleIdParcelle(UUID idParcelle);

    long countByStatutSignalementIdStatutSignalement(Integer idStatutSignalement);

    long countByTypeSignalementIdTypeSignalement(Integer idTypeSignalement);

    /**
     * Numéro max de référence pour un préfixe donné (ex : "SIG-2026-").
     * SUBSTRING(reference, start) extrait le suffixe numérique ("0001") puis on le caste en long.
     */
    @Query("SELECT MAX(CAST(SUBSTRING(s.reference, :start) AS long)) FROM Signalement s " +
           "WHERE s.reference LIKE CONCAT(:prefix, '%')")
    Long maxNumeroReference(@Param("prefix") String prefix, @Param("start") int start);
}