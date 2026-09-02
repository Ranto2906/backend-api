package com.seimad.patrimoine.repository.referentiel;

import com.seimad.patrimoine.entity.referentiel.PrixM2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrixM2Repository extends JpaRepository<PrixM2, Integer> {

    List<PrixM2> findByVilleIdVilleOrderByDateMajDesc(Integer idVille);

    List<PrixM2> findAllByOrderByDateMajDesc();
}
