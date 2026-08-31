package com.seimad.patrimoine.entity.dossier;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit")
    private Integer idAudit;

    @Column(name = "entite_type", nullable = false, length = 30)
    private String entiteType;

    @Column(name = "entite_id", nullable = false, columnDefinition = "TEXT")
    private String entiteId;

    @Column(name = "action", length = 20)
    private String action;

    @Column(name = "anciennes_valeurs", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String anciennesValeurs;

    @Column(name = "nouvelles_valeurs", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String nouvellesValeurs;

    @Column(name = "date_action")
    @Builder.Default
    private LocalDateTime dateAction = LocalDateTime.now();

    @Column(name = "ip_adresse")
    private InetAddress ipAdresse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;
}
