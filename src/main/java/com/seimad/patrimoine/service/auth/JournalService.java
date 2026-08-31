package com.seimad.patrimoine.service.auth;

import com.seimad.patrimoine.dto.auth.JournalConnexionDTO;
import com.seimad.patrimoine.entity.auth.JournalConnexion;
import com.seimad.patrimoine.entity.auth.SessionUtilisateur;
import com.seimad.patrimoine.repository.auth.JournalConnexionRepository;
import com.seimad.patrimoine.repository.auth.SessionUtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class JournalService {

    private final JournalConnexionRepository journalRepository;
    private final SessionUtilisateurRepository sessionRepository;

    public Page<JournalConnexionDTO> listerJournal(Pageable pageable) {
        return journalRepository.findAllByOrderByDateConnexionDesc(pageable)
                .map(this::toDTO);
    }

    public List<JournalConnexionDTO> listerParUtilisateur(Integer idUtilisateur) {
        return journalRepository
                .findByUtilisateurIdUtilisateurOrderByDateConnexionDesc(idUtilisateur)
                .stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void revoquerToutesLesSessions(Integer idUtilisateur) {
        List<SessionUtilisateur> sessions = sessionRepository
                .findByUtilisateurIdUtilisateurAndRevoqueFalse(idUtilisateur);

        for (SessionUtilisateur session : sessions) {
            session.setRevoque(true);
        }
        sessionRepository.saveAll(sessions);

        log.info("Toutes les sessions de l'utilisateur {} ont été révoquées", idUtilisateur);
    }

    @Transactional
    public void nettoyerSessionsExpirees() {
        sessionRepository.deleteByDateExpirationBefore(java.time.LocalDateTime.now());
        log.info("Sessions expirées nettoyées");
    }

    private JournalConnexionDTO toDTO(JournalConnexion j) {
        return JournalConnexionDTO.builder()
                .idJournal(j.getIdJournal())
                .dateConnexion(j.getDateConnexion() != null
                        ? j.getDateConnexion().toString() : null)
                .ipAdresse(j.getIpAdresse() != null
                        ? j.getIpAdresse().getHostAddress() : null)
                .succes(j.getSucces())
                .idUtilisateur(j.getUtilisateur() != null
                        ? j.getUtilisateur().getIdUtilisateur() : null)
                .nomUtilisateur(j.getUtilisateur() != null
                        ? j.getUtilisateur().getNomUtilisateur() : null)
                .build();
    }
}
