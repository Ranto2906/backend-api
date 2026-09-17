package com.seimad.patrimoine.controller.dossier;

import com.seimad.patrimoine.dto.dossier.DocumentDemandeurDTO;
import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.DocumentDemandeur;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.repository.auth.UtilisateurRepository;
import com.seimad.patrimoine.repository.dossier.DocumentDemandeurRepository;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Documents / photos joints aux personnes (demandeurs d'un dossier).
 * Table document_demandeur — upload multipart, stockage fichier + métadonnées.
 */
@RestController
@RequestMapping("/api/personnes-documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documents demandeurs", description = "Photos et documents joints aux personnes")
public class DocumentDemandeurController {

    private final DocumentDemandeurRepository documentDemandeurRepository;
    private final PersonneRepository personneRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${app.stockage-photos:uploads/photos}")
    private String stockage;

    /** Types du CHECK(type_document) en base. */
    private static final Set<String> TYPES_AUTORISES = Set.of(
            "photo_demandeur", "photo_identite", "pdf_piece",
            "PV", "Plan", "Quittance", "Acte", "Autre");

    private static final Set<String> EXTENSIONS_AUTORISEES = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf");

    @GetMapping("/personne/{idPersonne}")
    @Operation(summary = "Documents d'une personne")
    public ResponseEntity<List<DocumentDemandeurDTO>> lister(@PathVariable Integer idPersonne) {
        List<DocumentDemandeurDTO> docs = documentDemandeurRepository
                .findByPersonne_IdPersonneOrderByDateUploadAsc(idPersonne).stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Joindre un document / une photo à une personne",
            description = "Upload multipart : fichier + idPersonne + typeDocument")
    public ResponseEntity<?> ajouter(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam("idPersonne") Integer idPersonne,
            @RequestParam(value = "typeDocument", defaultValue = "Autre") String typeDocument) {

        Personne personne = personneRepository.findById(idPersonne).orElse(null);
        if (personne == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Personne non trouvée"));
        }
        if (!TYPES_AUTORISES.contains(typeDocument)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Type de document non autorisé : " + typeDocument));
        }
        if (fichier == null || fichier.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucun fichier fourni"));
        }
        String ext = extensionDe(fichier.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Format non supporté (images .jpg/.png/.gif/.webp ou PDF)"));
        }

        try {
            String nomStocke = "doc_pers_" + System.currentTimeMillis() + "_" + idPersonne + "." + ext;
            Path racine = Paths.get(stockage, "documents").toAbsolutePath();
            Files.createDirectories(racine);
            Files.write(racine.resolve(nomStocke), fichier.getBytes());

            Utilisateur utilisateur = utilisateurConnecte();
            DocumentDemandeur doc = documentDemandeurRepository.save(DocumentDemandeur.builder()
                    .nomFichier(fichier.getOriginalFilename())
                    .cheminFichier("documents/" + nomStocke)
                    .typeDocument(typeDocument)
                    .tailleOctets(fichier.getSize())
                    .utilisateur(utilisateur)
                    .personne(personne)
                    .build());

            log.info("Document ajouté à la personne {} : {} ({}, {} octets)",
                    idPersonne, doc.getTypeDocument(), doc.getNomFichier(), doc.getTailleOctets());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(doc));
        } catch (IOException e) {
            log.error("Écriture du document impossible : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Erreur lors de l'enregistrement du document"));
        }
    }

    @GetMapping("/{id}/contenu")
    @Operation(summary = "Contenu binaire d'un document (image inline ou PDF)")
    public ResponseEntity<byte[]> contenu(@PathVariable Integer id) {
        DocumentDemandeur doc = documentDemandeurRepository.findById(id).orElse(null);
        if (doc == null || doc.getCheminFichier() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path racine = Paths.get(stockage).toAbsolutePath();
            Path fichier = racine.resolve(doc.getCheminFichier()).normalize();
            if (!fichier.startsWith(racine) || !Files.exists(fichier)) {
                return ResponseEntity.notFound().build();
            }
            byte[] donnees = Files.readAllBytes(fichier);
            String ext = extensionDe(doc.getCheminFichier());
            MediaType type = "pdf".equals(ext) ? MediaType.APPLICATION_PDF : mediaTypeImageDe(ext);
            return ResponseEntity.ok()
                    .contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\""
                            + (doc.getNomFichier() != null ? doc.getNomFichier() : "document") + "\"")
                    .body(donnees);
        } catch (Exception e) {
            log.warn("Lecture document {} impossible : {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un document")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        DocumentDemandeur doc = documentDemandeurRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            if (doc.getCheminFichier() != null) {
                Path racine = Paths.get(stockage).toAbsolutePath();
                Path fichier = racine.resolve(doc.getCheminFichier()).normalize();
                if (fichier.startsWith(racine) && Files.exists(fichier)) {
                    Files.deleteIfExists(fichier);
                }
            }
        } catch (Exception e) {
            log.warn("Suppression du fichier du document {} impossible : {}", id, e.getMessage());
        }
        documentDemandeurRepository.delete(doc);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──

    private DocumentDemandeurDTO toDTO(DocumentDemandeur d) {
        return DocumentDemandeurDTO.builder()
                .idDocument(d.getIdDocument())
                .nomFichier(d.getNomFichier())
                .typeDocument(d.getTypeDocument())
                .tailleOctets(d.getTailleOctets())
                .dateUpload(d.getDateUpload())
                .idPersonne(d.getPersonne() != null ? d.getPersonne().getIdPersonne() : null)
                .nomPersonne(d.getPersonne() != null ? d.getPersonne().getNom() : null)
                .build();
    }

    private Utilisateur utilisateurConnecte() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }

    private String extensionDe(String nomFichier) {
        if (nomFichier == null) return "";
        int i = nomFichier.lastIndexOf('.');
        return i >= 0 ? nomFichier.substring(i + 1).toLowerCase() : "";
    }

    private MediaType mediaTypeImageDe(String ext) {
        return switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "jpeg", "jpg", "jpe" -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
