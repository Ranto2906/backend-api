package com.seimad.patrimoine.service.auth;

import com.seimad.patrimoine.dto.auth.*;
import com.seimad.patrimoine.entity.auth.*;
import com.seimad.patrimoine.repository.auth.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final ModuleRepository moduleRepository;
    private final EntiteRepository entiteRepository;
    private final ActionRepository actionRepository;

    public List<RoleDTO> lister() {
        return roleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public RoleDTO trouverParId(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Rôle non trouvé avec l'id : " + id));
        return toDTO(role);
    }

    @Transactional
    public RoleDTO creer(CreateRoleRequest request) {
        if (roleRepository.existsByNomRole(request.getNomRole())) {
            throw new IllegalArgumentException("Le rôle '" + request.getNomRole() + "' existe déjà");
        }
        Role role = Role.builder()
                .nomRole(request.getNomRole())
                .description(request.getDescription())
                .build();
        role = roleRepository.save(role);
        log.info("Rôle créé : {} (id={})", role.getNomRole(), role.getIdRole());
        return toDTO(role);
    }

    @Transactional
    public RoleDTO mettreAJour(Integer id, CreateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Rôle non trouvé avec l'id : " + id));
        if (!request.getNomRole().equals(role.getNomRole())
                && roleRepository.existsByNomRole(request.getNomRole())) {
            throw new IllegalArgumentException("Le rôle '" + request.getNomRole() + "' existe déjà");
        }
        role.setNomRole(request.getNomRole());
        role.setDescription(request.getDescription());
        role = roleRepository.save(role);
        log.info("Rôle mis à jour : {} (id={})", role.getNomRole(), id);
        return toDTO(role);
    }

    @Transactional
    public void supprimer(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Rôle non trouvé avec l'id : " + id));
        rolePermissionRepository.deleteAllByRoleIdRole(id);
        roleRepository.delete(role);
        log.info("Rôle supprimé : {} (id={})", role.getNomRole(), id);
    }

    public List<PermissionDTO> listerPermissions() {
        return permissionRepository.findAllWithDetails().stream()
                .map(this::toPermissionDTO)
                .collect(Collectors.toList());
    }

    public List<PermissionDTO> listerPermissionsParRole(Integer idRole) {
        roleRepository.findById(idRole)
                .orElseThrow(() -> new NoSuchElementException("Rôle non trouvé avec l'id : " + idRole));
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdRole(idRole);
        List<Integer> permissionIds = rolePermissions.stream()
                .map(RolePermission::getIdPermission)
                .collect(Collectors.toList());
        if (permissionIds.isEmpty()) return Collections.emptyList();
        return permissionRepository.findByIdInWithDetails(permissionIds).stream()
                .map(this::toPermissionDTO)
                .collect(Collectors.toList());
    }

    public RoleWithPermissionsDTO trouverAvecPermissions(Integer idRole) {
        Role role = roleRepository.findById(idRole)
                .orElseThrow(() -> new NoSuchElementException("Rôle non trouvé avec l'id : " + idRole));
        List<PermissionDTO> permissions = listerPermissionsParRole(idRole);
        return RoleWithPermissionsDTO.builder()
                .idRole(role.getIdRole())
                .nomRole(role.getNomRole())
                .description(role.getDescription())
                .permissions(permissions)
                .build();
    }

    @Transactional
    public void attribuerPermission(Integer idRole, Integer idPermission) {
        roleRepository.findById(idRole)
                .orElseThrow(() -> new NoSuchElementException("Rôle non trouvé"));
        permissionRepository.findById(idPermission)
                .orElseThrow(() -> new NoSuchElementException("Permission non trouvée"));
        if (rolePermissionRepository.existsByRoleIdRoleAndPermissionIdPermission(idRole, idPermission)) {
            throw new IllegalArgumentException("Cette permission est déjà attribuée à ce rôle");
        }
        rolePermissionRepository.save(RolePermission.builder()
                .idRole(idRole)
                .idPermission(idPermission)
                .build());
        log.info("Permission {} attribuée au rôle {}", idPermission, idRole);
    }

    @Transactional
    public void retirerPermission(Integer idRole, Integer idPermission) {
        rolePermissionRepository.deleteById(new RolePermissionId(idRole, idPermission));
        log.info("Permission {} retirée du rôle {}", idPermission, idRole);
    }

    public List<Map<String, Object>> listerModules() {
        return moduleRepository.findAll().stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("idModule", m.getIdModule());
                    map.put("codeModule", m.getCodeModule());
                    map.put("libelle", m.getLibelle());
                    return map;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> listerEntites() {
        return entiteRepository.findAll().stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("idEntite", e.getIdEntite());
                    map.put("codeEntite", e.getCodeEntite());
                    map.put("libelle", e.getLibelle());
                    map.put("codeModule", e.getModule().getCodeModule());
                    return map;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> listerActions() {
        return actionRepository.findAll().stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("idAction", a.getIdAction());
                    map.put("codeAction", a.getCodeAction());
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional
    public PermissionDTO creerPermission(Integer idModule, Integer idEntite, Integer idAction) {
        com.seimad.patrimoine.entity.auth.Module module = moduleRepository.findById(idModule)
                .orElseThrow(() -> new NoSuchElementException("Module non trouvé"));
        Entite entite = entiteRepository.findById(idEntite)
                .orElseThrow(() -> new NoSuchElementException("Entité non trouvée"));
        Action action = actionRepository.findById(idAction)
                .orElseThrow(() -> new NoSuchElementException("Action non trouvée"));
        Optional<Permission> existing = permissionRepository
                .findByEntiteIdEntiteAndActionIdAction(idEntite, idAction);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Cette permission existe déjà");
        }
        Permission permission = permissionRepository.save(Permission.builder()
                .action(action)
                .entite(entite)
                .build());
        return toPermissionDTO(permission);
    }

    private RoleDTO toDTO(Role role) {
        return RoleDTO.builder()
                .idRole(role.getIdRole())
                .nomRole(role.getNomRole())
                .description(role.getDescription())
                .build();
    }

    private PermissionDTO toPermissionDTO(Permission p) {
        return PermissionDTO.builder()
                .idPermission(p.getIdPermission())
                .codeModule(p.getEntite().getModule().getCodeModule())
                .libelleModule(p.getEntite().getModule().getLibelle())
                .codeEntite(p.getEntite().getCodeEntite())
                .libelleEntite(p.getEntite().getLibelle())
                .codeAction(p.getAction().getCodeAction())
                .build();
    }
}
