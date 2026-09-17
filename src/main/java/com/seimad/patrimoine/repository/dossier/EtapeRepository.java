package com.seimad.patrimoine.repository.dossier;

import com.seimad.patrimoine.entity.dossier.Etape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtapeRepository extends JpaRepository<Etape, Integer> {

    List<Etape> findAllByOrderByOrdreAsc();

    Optional<Etape> findByCodeEtape(String codeEtape);

    Optional<Etape> findByOrdre(Integer ordre);

    boolean existsByCodeEtape(String codeEtape);

    boolean existsByOrdre(Integer ordre);
}
