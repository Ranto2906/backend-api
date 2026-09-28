package com.seimad.patrimoine.controller.statistique;

import com.seimad.patrimoine.dto.statistique.TableauBordDTO;
import com.seimad.patrimoine.service.statistique.TableauBordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * API du tableau de bord : une seule synthèse (KPI, répartitions, séries
 * mensuelles, finances, alertes, activité) pour alimenter la page d'accueil.
 */
@RestController
@RequestMapping("/api/statistiques")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistiques", description = "Synthèses et indicateurs du patrimoine foncier")
public class TableauBordController {

    private final TableauBordService tableauBordService;

    @GetMapping("/tableau-bord")
    @Operation(summary = "Synthèse complète du tableau de bord (KPI, graphiques, alertes)",
            description = "Filtre optionnel dateDebut/dateFin (ISO yyyy-MM-dd) sur toutes les données datées")
    public ResponseEntity<TableauBordDTO> tableauBord(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(tableauBordService.tableauBord(dateDebut, dateFin));
    }
}
