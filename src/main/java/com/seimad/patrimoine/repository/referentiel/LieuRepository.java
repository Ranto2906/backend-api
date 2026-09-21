package com.seimad.patrimoine.repository.referentiel;

import com.seimad.patrimoine.entity.referentiel.Lieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LieuRepository extends JpaRepository<Lieu, Integer> {

    List<Lieu> findByVilleIdVilleOrderByLibelleAsc(Integer idVille);

    Optional<Lieu> findByVilleIdVilleAndLibelleIgnoreCase(Integer idVille, String libelle);

    List<Lieu> findByLibelleIgnoreCase(String libelle);
}