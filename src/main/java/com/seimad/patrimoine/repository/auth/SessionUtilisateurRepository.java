package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.SessionUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionUtilisateurRepository extends JpaRepository<SessionUtilisateur, Integer> {

    Optional<SessionUtilisateur> findByTokenRafraichissementAndRevoqueFalse(String token);

    List<SessionUtilisateur> findByUtilisateurIdUtilisateurAndRevoqueFalse(Integer idUtilisateur);

    void deleteByDateExpirationBefore(LocalDateTime date);

    void deleteByUtilisateurIdUtilisateurAndRevoqueFalse(Integer idUtilisateur);
}
