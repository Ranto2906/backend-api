package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.Personne;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonneRepository extends JpaRepository<Personne, Integer> {

    List<Personne> findByRole(String role);

    List<Personne> findByNomContainingIgnoreCase(String nom);

    @Query("SELECT p FROM Personne p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.nom) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(p.contact) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(p.adresse) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Personne> search(@Param("search") String search, Pageable pageable);
}
