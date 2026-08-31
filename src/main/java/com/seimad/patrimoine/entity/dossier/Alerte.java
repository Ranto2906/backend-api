package com.seimad.patrimoine.entity.dossier;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerte")
    private Integer idAlerte;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "date_envoi")
    @Builder.Default
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "destinataire", length = 200)
    private String destinataire;

    @Column(name = "lu")
    @Builder.Default
    private Boolean lu = false;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;
}
