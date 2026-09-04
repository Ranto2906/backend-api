package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.TypePersonne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypePersonneRepository extends JpaRepository<TypePersonne, Integer> {

    Optional<TypePersonne> findByCode(String code);

    List<TypePersonne> findAllByOrderByLibelleAsc();
}