package com.seimad.patrimoine.service.dossier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seimad.patrimoine.dto.dossier.AuditDTO;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.Audit;
import com.seimad.patrimoine.repository.dossier.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    // ── Enregistrer une action d'audit ──

    @Transactional
    public void enregistrer(String entiteType, String entiteId, String action,
                            Map<String, Object> anciennesValeurs,
                            Map<String, Object> nouvellesValeurs,
                            Utilisateur utilisateur,
                            HttpServletRequest request) {
        try {
            Audit audit = Audit.builder()
                    .entiteType(entiteType)
                    .entiteId(entiteId)
                    .action(action)
                    .anciennesValeurs(anciennesValeurs != null ? objectMapper.writeValueAsString(anciennesValeurs) : null)
                    .nouvellesValeurs(nouvellesValeurs != null ? objectMapper.writeValueAsString(nouvellesValeurs) : null)
                    .utilisateur(utilisateur)
                    .ipAdresse(getClientIp(request))
                    .build();
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'audit : {}", e.getMessage());
        }
    }

    // Surcharge sans HttpServletRequest (pour les appels internes)
    @Transactional
    public void enregistrer(String entiteType, String entiteId, String action,
                            Map<String, Object> anciennesValeurs,
                            Map<String, Object> nouvellesValeurs,
                            Utilisateur utilisateur) {
        enregistrer(entiteType, entiteId, action, anciennesValeurs, nouvellesValeurs, utilisateur, null);
    }

    // Surcharge sans HttpServletRequest ni Utilisateur (pour les appels RBAC)
    @Transactional
    public void enregistrer(String entiteType, String entiteId, String action,
                            Map<String, Object> anciennesValeurs,
                            Map<String, Object> nouvellesValeurs) {
        enregistrer(entiteType, entiteId, action, anciennesValeurs, nouvellesValeurs, null, null);
    }

    // ── Lecture ──

    public Page<AuditDTO> lister(Pageable pageable) {
        return auditRepository.findAllByOrderByDateActionDesc(pageable)
                .map(this::toDTO);
    }

    public Page<AuditDTO> rechercher(String entiteType, String action, String search, Pageable pageable) {
        return auditRepository.search(entiteType, action, search, pageable)
                .map(this::toDTO);
    }

    public long countByEntiteType(String entiteType) {
        return auditRepository.countByEntiteType(entiteType);
    }

    public long countByAction(String action) {
        return auditRepository.countByAction(action);
    }

    // ── Helpers ──

    private InetAddress getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        try {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty())
                return InetAddress.getByName(xForwardedFor.split(",")[0].trim());
            return InetAddress.getByName(request.getRemoteAddr());
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private AuditDTO toDTO(Audit a) {
        return AuditDTO.builder()
                .idAudit(a.getIdAudit())
                .entiteType(a.getEntiteType())
                .entiteId(a.getEntiteId())
                .action(a.getAction())
                .anciennesValeurs(a.getAnciennesValeurs())
                .nouvellesValeurs(a.getNouvellesValeurs())
                .dateAction(a.getDateAction() != null ? a.getDateAction().toString() : null)
                .ipAdresse(a.getIpAdresse() != null ? a.getIpAdresse().getHostAddress() : null)
                .idUtilisateur(a.getUtilisateur() != null ? a.getUtilisateur().getIdUtilisateur() : null)
                .nomUtilisateur(a.getUtilisateur() != null ? a.getUtilisateur().getNomUtilisateur() : null)
                .build();
    }
}
