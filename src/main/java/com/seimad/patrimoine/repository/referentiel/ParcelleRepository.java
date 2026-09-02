package com.seimad.patrimoine.repository.referentiel;

import com.seimad.patrimoine.entity.referentiel.Parcelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParcelleRepository extends JpaRepository<Parcelle, UUID> {

    List<Parcelle> findByTitreFoncierIdTitreFoncier(UUID idTitreFoncier);
}
