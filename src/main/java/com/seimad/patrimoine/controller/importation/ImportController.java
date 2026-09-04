package com.seimad.patrimoine.controller.importation;

import com.seimad.patrimoine.service.notification.ExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Import Excel", description = "Import des données depuis un fichier Excel")
public class ImportController {

    private final ExcelImportService excelImportService;

    @PostMapping("/excel")
    @Operation(summary = "Importer le fichier Excel « Suivi des notifications Patrimoine foncier »",
            description = "Feuille 1 : notifications d'occupation (+ suivis). Feuille 2 : avertissements. "
                    + "Les référentiels (propriété, titre foncier, parcelle) et les personnes sont créés si absents.")
    public ResponseEntity<Map<String, Object>> importerExcel(
            @RequestParam("file") MultipartFile fichier) throws IOException {
        if (fichier == null || fichier.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "Aucun fichier reçu"));
        }
        Map<String, Object> resume = excelImportService.importer(fichier);
        return ResponseEntity.ok(resume);
    }
}