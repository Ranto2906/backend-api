package com.seimad.patrimoine.entity.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "action")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_action")
    private Integer idAction;

    @Column(name = "code_action", nullable = false, unique = true, length = 50)
    private String codeAction;
}
