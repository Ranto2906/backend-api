package com.seimad.patrimoine.repository.signalement;

import com.seimad.patrimoine.entity.signalement.TypeSignalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeSignalementRepository extends JpaRepository<TypeSignalement, Integer> {
}