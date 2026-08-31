package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permission")
@IdClass(RolePermissionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @Column(name = "id_role")
    private Integer idRole;

    @Id
    @Column(name = "id_permission")
    private Integer idPermission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role", insertable = false, updatable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_permission", insertable = false, updatable = false)
    private Permission permission;
}
