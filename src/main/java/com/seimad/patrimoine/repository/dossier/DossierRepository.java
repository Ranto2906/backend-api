package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.Dossier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Integer> {

    Optional<Dossier> findByNumeroDossier(String numeroDossier);

    Optional<Dossier> findFirstByNumeroDossierOrderByDateCreationDesc(String numeroDossier);

    boolean existsByNumeroDossier(String numeroDossier);

    /**
     * Recherche multi-critères : texte libre (numéro, résultat d'étude, observation,
     * ville, transmission, demandeur), statut, ville, année, période.
     */
    @Query("""
            SELECT DISTINCT d FROM Dossier d
            LEFT JOIN d.ville v
            LEFT JOIN DossierDemandeur dd ON dd.idDossier = d.idDossier
            LEFT JOIN Personne p ON p.idPersonne = dd.idPersonne
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(d.numeroDossier) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(d.resultatEtude, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(d.observation, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(d.transmission, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(v.nomVille, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(p.nom, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:idStatut IS NULL OR d.statutDossier.idStatutDossier = :idStatut)
              AND (:idVille IS NULL OR d.ville.idVille = :idVille)
              AND (:annee IS NULL OR d.annee = :annee)
              AND (:dateDebut IS NULL OR d.dateDemande >= :dateDebut)
              AND (:dateFin IS NULL OR d.dateDemande <= :dateFin)
            ORDER BY d.dateCreation DESC
            """)
    Page<Dossier> search(@Param("search") String search,
                         @Param("idStatut") Integer idStatut,
                         @Param("idVille") Integer idVille,
                         @Param("annee") Integer annee,
                         @Param("dateDebut") java.time.LocalDate dateDebut,
                         @Param("dateFin") java.time.LocalDate dateFin,
                         Pageable pageable);

    long countByAnnee(Integer annee);

    @Query("SELECT COUNT(d) FROM Dossier d WHERE d.statutDossier.estFinal = true")
    long countDossiersFinalises();

    @Query("SELECT d.statutDossier.libelle, COUNT(d) FROM Dossier d " +
           "WHERE d.statutDossier IS NOT NULL GROUP BY d.statutDossier.libelle")
    List<Object[]> countParStatut();
}
