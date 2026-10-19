package com.seimad.patrimoine.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_propriete")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypePropriete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_propriete")
    private Integer idTypePropriete;

    @Column(name = "libelle", nullable = false, length = 80, unique = true)
    private String libelle;

    @Override
    public String toString() {
        return "TypePropriete{" +
               "idTypePropriete=" + idTypePropriete +
               ", libelle='" + libelle + '\'' +
               '}';
    }
}
