package com.seimad.patrimoine.repository.referentiel;

import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TitreFoncierRepository extends JpaRepository<TitreFoncier, UUID> {

    Optional<TitreFoncier> findByNumero(String numero);
}
