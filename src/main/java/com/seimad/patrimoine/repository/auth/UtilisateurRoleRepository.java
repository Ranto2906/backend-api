package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.UtilisateurRole;
import com.seimad.patrimoine.entity.auth.UtilisateurRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UtilisateurRoleRepository extends JpaRepository<UtilisateurRole, UtilisateurRoleId> {

    List<UtilisateurRole> findByUtilisateurIdUtilisateur(Integer idUtilisateur);

    List<UtilisateurRole> findByRoleIdRole(Integer idRole);

    boolean existsByUtilisateurIdUtilisateurAndRoleIdRole(Integer idUtilisateur, Integer idRole);

    void deleteByUtilisateurIdUtilisateurAndRoleIdRole(Integer idUtilisateur, Integer idRole);
}
