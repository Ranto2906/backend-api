package com.seimad.patrimoine.service.dossier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seimad.patrimoine.dto.dossier.AuditDTO;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.Audit;
import com.seimad.patrimoine.repository.dossier.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate auditTransaction;

    public AuditService(AuditRepository auditRepository,
                        ObjectMapper objectMapper,
                        PlatformTransactionManager transactionManager) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
        // Transaction indépendante (REQUIRES_NEW) : un échec d'audit ne peut
        // jamais marquer la transaction métier appelante rollback-only.
        this.auditTransaction = new TransactionTemplate(transactionManager);
        this.auditTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ── Enregistrer une action d'audit ──

    /**
     * Enregistre l'audit dans une transaction indépendante : en cas d'échec
     * (contrainte non étendue, base indisponible), l'erreur est loguée et
     * l'opération métier appelante réussit normalement.
     */
    public void enregistrer(String entiteType, String entiteId, String action,
                            Map<String, Object> anciennesValeurs,
                            Map<String, Object> nouvellesValeurs,
                            Utilisateur utilisateur,
                            HttpServletRequest request) {
        try {
            auditTransaction.executeWithoutResult(status -> {
                Audit audit = Audit.builder()
                        .entiteType(entiteType)
                        .entiteId(entiteId)
                        .action(action)
                        .anciennesValeurs(anciennesValeurs != null ? writeJson(anciennesValeurs) : null)
                        .nouvellesValeurs(nouvellesValeurs != null ? writeJson(nouvellesValeurs) : null)
                        .utilisateur(utilisateur)
                        .ipAdresse(getClientIp(request))
                        .build();
                auditRepository.save(audit);
            });
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'audit ({} #{}) : {}",
                    entiteType, entiteId, e.getMessage());
        }
    }

    // Surcharge sans HttpServletRequest (pour les appels internes)
    public void enregistrer(String entiteType, String entiteId, String action,
                            Map<String, Object> anciennesValeurs,
                            Map<String, Object> nouvellesValeurs,
                            Utilisateur utilisateur) {
        enregistrer(entiteType, entiteId, action, anciennesValeurs, nouvellesValeurs, utilisateur, null);
    }

    // Surcharge sans HttpServletRequest ni Utilisateur (pour les appels RBAC)
    public void enregistrer(String entiteType, String entiteId, String action,
                            Map<String, Object> anciennesValeurs,
                            Map<String, Object> nouvellesValeurs) {
        enregistrer(entiteType, entiteId, action, anciennesValeurs, nouvellesValeurs, null, null);
    }

    private String writeJson(Map<String, Object> valeurs) {
        try {
            return objectMapper.writeValueAsString(valeurs);
        } catch (Exception e) {
            return String.valueOf(valeurs);
        }
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

    // ── Historique d'une entité spécifique ──

    public List<AuditDTO> historiqueEntite(String entiteType, String entiteId) {
        return auditRepository.findByEntiteTypeAndEntiteIdOrderByDateActionDesc(entiteType, entiteId)
                .stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public AuditDTO derniereModification(String entiteType, String entiteId) {
        List<Audit> audits = auditRepository.findByEntiteTypeAndEntiteIdOrderByDateActionDesc(entiteType, entiteId);
        return audits.isEmpty() ? null : toDTO(audits.get(0));
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
