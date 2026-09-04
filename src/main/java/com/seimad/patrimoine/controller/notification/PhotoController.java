package com.seimad.patrimoine.controller.notification;

import com.seimad.patrimoine.dto.notification.PhotoDTO;
import com.seimad.patrimoine.entity.notification.Photo;
import com.seimad.patrimoine.repository.notification.PhotoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Photos", description = "Photos des notifications, suivis et avertissements")
public class PhotoController {

    private final PhotoRepository photoRepository;

    @Value("${app.stockage-photos:uploads/photos}")
    private String stockagePhotos;

    private static final Set<String> ENTITES_AUTORISEES = Set.of(
            "notification", "suivi", "avertissement", "signalement", "descente_terrain");

    private static final Set<String> EXTENSIONS_AUTORISEES = Set.of(
            "jpg", "jpeg", "png", "gif", "webp");

    @GetMapping
    @Operation(summary = "Lister les photos d'une entité (notification / suivi / avertissement)")
    public ResponseEntity<List<PhotoDTO>> lister(
            @RequestParam String entiteType,
            @RequestParam UUID entiteId) {
        List<PhotoDTO> photos = photoRepository
                .findByEntiteTypeAndEntiteIdOrderByDateCreationAsc(entiteType, entiteId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(photos);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Joindre une photo à une entité",
            description = "Upload multipart : fichier + entiteType + entiteId + typePhoto/datePrise/observation optionnels")
    public ResponseEntity<?> ajouter(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam String entiteType,
            @RequestParam UUID entiteId,
            @RequestParam(required = false) String typePhoto,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datePrise,
            @RequestParam(required = false) String observation) {
        if (!ENTITES_AUTORISEES.contains(entiteType)) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Type d'entité non autorisé : " + entiteType));
        }
        if (fichier == null || fichier.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucun fichier fourni"));
        }
        String ext = extensionDe(fichier.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Format d'image non supporté (.jpg, .png, .gif ou .webp)"));
        }

        try {
            UUID idPhoto = UUID.randomUUID();
            String nomFichier = idPhoto + "." + ext;

            Path racine = Paths.get(stockagePhotos).toAbsolutePath();
            Files.createDirectories(racine);
            Files.write(racine.resolve(nomFichier), fichier.getBytes());

            Photo photo = photoRepository.save(Photo.builder()
                    .idPhoto(idPhoto)
                    .entiteType(entiteType)
                    .entiteId(entiteId)
                    .typePhoto(typePhoto)
                    .observation(nettoyer(observation))
                    .datePrise(datePrise)
                    .cheminFichier(nomFichier)
                    .build());
            log.info("Photo ajoutée : {}/{} ({}, {} octets)", entiteType, entiteId, typePhoto,
                    fichier.getSize());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(photo));
        } catch (IOException e) {
            log.error("Écriture de la photo impossible : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Erreur lors de l'enregistrement de la photo"));
        }
    }

    @GetMapping("/{id}/contenu")
    @Operation(summary = "Contenu binaire d'une photo")
    public ResponseEntity<byte[]> contenu(@PathVariable UUID id) {
        Photo photo = photoRepository.findById(id).orElse(null);
        if (photo == null || photo.getCheminFichier() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path racine = Paths.get(stockagePhotos).toAbsolutePath();
            Path fichier = racine.resolve(photo.getCheminFichier()).normalize();
            if (!fichier.startsWith(racine) || !Files.exists(fichier)) {
                return ResponseEntity.notFound().build();
            }
            byte[] donnees = Files.readAllBytes(fichier);
            String ext = extensionDe(photo.getCheminFichier());
            MediaType type = mediaTypeDe(ext);
            return ResponseEntity.ok()
                    .contentType(type)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getCheminFichier() + "\"")
                    .body(donnees);
        } catch (Exception e) {
            log.warn("Lecture photo {} impossible : {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private PhotoDTO toDTO(Photo p) {
        return PhotoDTO.builder()
                .idPhoto(p.getIdPhoto())
                .entiteType(p.getEntiteType())
                .entiteId(p.getEntiteId())
                .typePhoto(p.getTypePhoto())
                .observation(p.getObservation())
                .datePrise(p.getDatePrise())
                .dateCreation(p.getDateCreation())
                .build();
    }

    private String extensionDe(String nomFichier) {
        if (nomFichier == null) return "";
        int i = nomFichier.lastIndexOf('.');
        return i >= 0 ? nomFichier.substring(i + 1).toLowerCase() : "";
    }

    private String nettoyer(String valeur) {
        if (valeur == null) return null;
        String v = valeur.trim();
        return v.isEmpty() ? null : v;
    }

    private MediaType mediaTypeDe(String ext) {
        return switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "jpeg", "jpg", "jpe" -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
