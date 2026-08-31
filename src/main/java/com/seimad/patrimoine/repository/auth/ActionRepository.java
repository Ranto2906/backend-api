package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActionRepository extends JpaRepository<Action, Integer> {

    Optional<Action> findByCodeAction(String codeAction);

    boolean existsByCodeAction(String codeAction);
}
