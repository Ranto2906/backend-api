package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByEntiteIdEntiteAndActionIdAction(Integer idEntite, Integer idAction);

    @Query("SELECT p FROM Permission p " +
           "JOIN FETCH p.entite e " +
           "JOIN FETCH e.module m " +
           "JOIN FETCH p.action a " +
           "ORDER BY m.codeModule, e.codeEntite, a.codeAction")
    List<Permission> findAllWithDetails();

    @Query("SELECT p FROM Permission p " +
           "JOIN FETCH p.entite e " +
           "JOIN FETCH e.module m " +
           "JOIN FETCH p.action a " +
           "WHERE m.idModule = :idModule")
    List<Permission> findByModuleId(@Param("idModule") Integer idModule);

    @Query("SELECT p FROM Permission p " +
           "JOIN FETCH p.entite e " +
           "JOIN FETCH e.module m " +
           "JOIN FETCH p.action a " +
           "WHERE p.idPermission IN :ids")
    List<Permission> findByIdInWithDetails(@Param("ids") List<Integer> ids);
}
