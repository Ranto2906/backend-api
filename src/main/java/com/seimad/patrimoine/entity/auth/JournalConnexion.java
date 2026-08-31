package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_connexion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalConnexion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_journal")
    private Integer idJournal;

    @Column(name = "date_connexion")
    @Builder.Default
    private LocalDateTime dateConnexion = LocalDateTime.now();

    @Column(name = "ip_adresse")
    private InetAddress ipAdresse;

    @Column(name = "succes")
    @Builder.Default
    private Boolean succes = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;
}
