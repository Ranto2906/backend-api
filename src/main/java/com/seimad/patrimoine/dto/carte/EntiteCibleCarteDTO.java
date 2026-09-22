package com.seimad.patrimoine.dto.carte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité ciblable depuis la carte interactive pour y définir son emplacement
 * (point ou polygone) : propriété ou parcelle, avec le drapeau
 * {@code localisee} indiquant si une géométrie lui est déjà rattachée.
 * L'identifiant est en texte : id entier pour une propriété (converti selon la
 * convention {@code 00000000-0000-0000-0000-{id:012d}}), UUID pour une parcelle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntiteCibleCarteDTO {

    /** Identifiant à transmettre à l'enregistrement de la géométrie. */
    private String id;

    /** Libellé principal (nom de la propriété, n° de lot de la parcelle). */
    private String libelle;

    /** Complément affiché (numéro, zone, lieu, ville, propriété rattachée…). */
    private String details;

    /** 'propriete' ou 'parcelle'. */
    private String type;

    /** true si une géométrie est déjà enregistrée pour cette entité. */
    private Boolean localisee;
}
