package com.seimad.patrimoine.repository.auth;

import com.seimad.patrimoine.entity.auth.RolePermission;
import com.seimad.patrimoine.entity.auth.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRoleIdRole(Integer idRole);

    List<RolePermission> findByPermissionIdPermission(Integer idPermission);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.idRole = :idRole")
    void deleteAllByRoleIdRole(@Param("idRole") Integer idRole);

    boolean existsByRoleIdRoleAndPermissionIdPermission(Integer idRole, Integer idPermission);
}
