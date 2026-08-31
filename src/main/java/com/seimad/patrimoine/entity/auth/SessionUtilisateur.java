package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_session")
    private Integer idSession;

    @Column(name = "token_rafraichissement", nullable = false, unique = true)
    private String tokenRafraichissement;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip_adresse")
    private InetAddress ipAdresse;

    @Column(name = "date_creation")
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "revoque")
    @Builder.Default
    private Boolean revoque = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;
}
