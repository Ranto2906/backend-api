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

    /**
     * Recherche multi-critères (référence, description, type, statut, ville,
     * n° titre, n° lot, créateur). LEFT JOIN explicites : les relations
     * optionnelles (ville, titre, parcelle) ne doivent pas exclure la ligne.
     */
    @Query("SELECT s FROM Signalement s " +
           "LEFT JOIN s.typeSignalement ts " +
           "LEFT JOIN s.statutSignalement st " +
           "LEFT JOIN s.ville ville " +
           "LEFT JOIN s.propriete tf " +
           "LEFT JOIN s.parcelle parc " +
           "LEFT JOIN s.utilisateurCreation uc " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "LOWER(s.reference) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(ts.libelle) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(st.libelle) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(ville.nomVille) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(tf.numero) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(parc.numeroLot) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(uc.nomComplet) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Signalement> search(@Param("search") String search, Pageable pageable);

    List<Signalement> findByVilleIdVille(Integer idVille);

    List<Signalement> findByStatutSignalementIdStatutSignalement(Integer idStatutSignalement);

    List<Signalement> findByTypeSignalementIdTypeSignalement(Integer idTypeSignalement);

       List<Signalement> findByProprieteIdPropriete(Integer idPropriete);

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