package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByNomUtilisateur(String nomUtilisateur);

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByNomUtilisateur(String nomUtilisateur);

    boolean existsByEmail(String email);

    List<Utilisateur> findByActifTrue();

    List<Utilisateur> findByStatutCompte(String statutCompte);

    @Query("SELECT u FROM Utilisateur u WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(u.nomUtilisateur) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(u.nomComplet) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Utilisateur> search(@Param("search") String search, Pageable pageable);
}
