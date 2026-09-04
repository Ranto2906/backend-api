package com.seimad.patrimoine.repository.signalement;

import com.seimad.patrimoine.entity.signalement.StatutSignalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatutSignalementRepository extends JpaRepository<StatutSignalement, Integer> {
}