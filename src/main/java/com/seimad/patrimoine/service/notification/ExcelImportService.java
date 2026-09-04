package com.seimad.patrimoine.service.notification;

import com.seimad.patrimoine.entity.notification.Avertissement;
import com.seimad.patrimoine.entity.notification.NotificationOccupation;
import com.seimad.patrimoine.entity.notification.NotificationPersonne;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.entity.notification.Photo;
import com.seimad.patrimoine.entity.notification.SuiviNotification;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.entity.referentiel.TitreFoncier;
import com.seimad.patrimoine.repository.notification.AvertissementRepository;
import com.seimad.patrimoine.repository.notification.NotificationOccupationRepository;
import com.seimad.patrimoine.repository.notification.NotificationPersonneRepository;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
import com.seimad.patrimoine.repository.notification.PhotoRepository;
import com.seimad.patrimoine.repository.notification.SuiviNotificationRepository;
import com.seimad.patrimoine.repository.notification.TypePersonneRepository;
import com.seimad.patrimoine.repository.referentiel.ParcelleRepository;
import com.seimad.patrimoine.repository.referentiel.ProprieteRepository;
import com.seimad.patrimoine.repository.referentiel.TitreFoncierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Import du fichier Excel « Suivi des notifications Patrimoine foncier ».
 * <p>
 * La feuille 1 (« Notifié ») contient les notifications d'occupation
 * (avec 1 ou 2 suivis par notification). La feuille 2 (« Autres Non notifié »)
 * contient les avertissements verbaux.
 * <p>
 * Les référentiels (propriété, titre foncier, parcelle) et les personnes sont
 * recherchés par leur nom/numéro ; s'ils n'existent pas encore, ils sont créés.
 * <p>
 * Depuis la V9 :
 * <ul>
 *   <li>import <b>idempotent</b> : une notification/avertissement déjà présent
 *       (même titre + parcelle + date + personne) n'est pas ré-importé
 *       (compteur « lignesIgnorees ») ;</li>
 *   <li>les <b>photos embarquées</b> du classeur sont extraites et rattachées
 *       (photo.entite_type : notification / suivi / avertissement) ;</li>
 *   <li>les occupants nommés dans « Informations occupants » sont créés en tant
 *       que Personne de rôle « Occupant » et liés à la notification.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelImportService {

    private final NotificationOccupationRepository notificationRepository;
    private final NotificationPersonneRepository notificationPersonneRepository;
    private final SuiviNotificationRepository suiviRepository;
    private final AvertissementRepository avertissementRepository;
    private final PersonneRepository personneRepository;
    private final TypePersonneRepository typePersonneRepository;
    private final PhotoRepository photoRepository;
    private final ProprieteRepository proprieteRepository;
    private final TitreFoncierRepository titreFoncierRepository;
    private final ParcelleRepository parcelleRepository;

    @Value("${app.stockage-photos:uploads/photos}")
    private String stockagePhotos;

    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final Pattern CONVOCATION_DATE = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{2,4})");
    private static final Pattern CONVOCATION_HEURE = Pattern.compile("(\\d{1,2})h");

    // ── Indices de colonnes — Feuille 1 « Notifié » ──
    private static final int S1_N            = 0;
    private static final int S1_PROPRIETE    = 1;
    private static final int S1_TITRE        = 2;
    private static final int S1_PARCELLE     = 3;
    private static final int S1_ZONE         = 4;
    private static final int S1_LOCALISATION = 5;
    private static final int S1_DATE_NOTIF   = 6;
    private static final int S1_ANNEE        = 7;
    private static final int S1_PERSONNE     = 8;
    private static final int S1_ADRESSE      = 9;
    private static final int S1_CONVOCATION  = 10;
    private static final int S1_INFOS        = 11;
    private static final int S1_CONSTATS     = 12;
    private static final int S1_DOLEANCES    = 13;
    private static final int S1_ACTIONS      = 14;
    private static final int S1_PHOTO_NOTIF  = 15;  // colonne P « Photos à la date de notification »
    private static final int S1_SUIVI1_DATE  = 16;
    private static final int S1_SUIVI1_CONSTATS = 17;
    private static final int S1_SUIVI1_ACTIONS  = 18;
    private static final int S1_SUIVI2_DATE  = 19;
    private static final int S1_SUIVI2_CONSTATS = 20;
    private static final int S1_PHOTO_SUIVI  = 21;  // colonne V « Photos à la date de suivi »

    // ── Indices de colonnes — Feuille 2 « Autres Non notifié » ──
    private static final int S2_PROPRIETE    = 1;
    private static final int S2_TITRE        = 2;
    private static final int S2_PARCELLE     = 3;
    private static final int S2_ZONE         = 4;
    private static final int S2_LOCALISATION = 5;
    private static final int S2_DATE_AVERT   = 6;
    private static final int S2_ANNEE        = 7;
    private static final int S2_PERSONNE     = 8;
    private static final int S2_INFOS        = 9;
    private static final int S2_CONSTATS     = 10;
    private static final int S2_ACTIONS      = 11;
    private static final int S2_PHOTO_CONSTAT = 12; // colonne M « Photos à la date de constatation »
    private static final int S2_PHOTO_SAT_APRES = 13; // colonne N « Photo satellitaire après construction »
    private static final int S2_PHOTO_SAT_AVANT = 14; // colonne O « Photo satellitaire avant construction »
    private static final int S2_A_FAIRE      = 15;
    private static final int S2_MISSION      = 16;

    // ── Rôles de personnes ──
    private static final String ROLE_NOTIFIE = "Notifié";
    private static final String ROLE_AVERTI  = "Averti";
    private static final String ROLE_OCCUPANT = "Occupant";

    /** Ligne source du classeur → entités créées/trouvées (pour le rattachement des photos). */
    private static class CiblesLigne {
        UUID notificationId;
        UUID suivi1Id;
        UUID suivi2Id;
    }

    /**
     * Importe le fichier Excel et renvoie un résumé des lignes traitées.
     */
    @Transactional
    public Map<String, Object> importer(MultipartFile fichier) throws IOException {
        Map<String, Object> resume = new LinkedHashMap<>();
        List<String> erreurs = new ArrayList<>();
        resume.put("notificationsImportees", 0);
        resume.put("avertissementsImportes", 0);
        resume.put("personnesCrees", 0);
        resume.put("personnesReutilisees", 0);
        resume.put("titresFonciersCrees", 0);
        resume.put("parcellesCrees", 0);
        resume.put("suivisCrees", 0);
        resume.put("photosImportees", 0);
        resume.put("lignesIgnorees", 0);
        resume.put("erreurs", erreurs);

        // Cache des référentiels/personnes déjà créés ou trouvés pendant l'import
        Map<String, Propriete> proprietes = new HashMap<>();
        Map<String, TitreFoncier> titres = new HashMap<>();
        Map<String, Parcelle> parcelles = new HashMap<>();
        Map<String, Personne> personnes = new HashMap<>();

        try (InputStream in = fichier.getInputStream();
             Workbook workbook = new XSSFWorkbook(in)) {

            if (workbook.getNumberOfSheets() > 0) {
                importerNotifications(workbook.getSheetAt(0), proprietes, titres, parcelles, personnes, resume, erreurs);
            }
            if (workbook.getNumberOfSheets() > 1) {
                importerAvertissements(workbook.getSheetAt(1), proprietes, titres, parcelles, personnes, resume, erreurs);
            }
        }

        return resume;
    }

    // ─────────────────────────────────────────────────────────────
    // Feuille 1 : notifications d'occupation
    // ─────────────────────────────────────────────────────────────

    private void importerNotifications(Sheet feuille,
                                       Map<String, Propriete> proprietes,
                                       Map<String, TitreFoncier> titres,
                                       Map<String, Parcelle> parcelles,
                                       Map<String, Personne> personnes,
                                       Map<String, Object> resume,
                                       List<String> erreurs) {
        int ligneEnTete = trouverLigneEnTete(feuille, 0, S1_TITRE);
        Map<Integer, CiblesLigne> cibles = new HashMap<>();
        int nb = 0;

        for (int i = ligneEnTete + 1; i <= feuille.getLastRowNum(); i++) {
            Row row = feuille.getRow(i);
            if (row == null) continue;

            String numero = texte(row.getCell(S1_N));
            if (numero == null || numero.isEmpty()) continue; // ligne vide

            try {
                TitreFoncier titreFoncier = obtenirTitreFoncier(
                        texte(row.getCell(S1_TITRE)),
                        texte(row.getCell(S1_PROPRIETE)),
                        texte(row.getCell(S1_ZONE)),
                        texte(row.getCell(S1_LOCALISATION)),
                        proprietes, titres);
                if (titreFoncier == null) {
                    erreurs.add("Ligne " + (i + 1) + " : titre foncier manquant — ignorée");
                    continue;
                }

                Parcelle parcelle = obtenirParcelle(
                        texte(row.getCell(S1_PARCELLE)), titreFoncier, parcelles);

                String nomPersonne = texte(row.getCell(S1_PERSONNE));
                String nomAdresse = texte(row.getCell(S1_ADRESSE));
                String infosOccupants = texte(row.getCell(S1_INFOS));
                LocalDate dateNotif = date(row.getCell(S1_DATE_NOTIF));

                // 1) Personne recherchée (sans création) pour la détection de doublon
                Personne personne = trouverPersonne(nomPersonne, personnes);

                // 2) Idempotence : la notification existe déjà ?
                NotificationOccupation existante = notificationDoublon(
                        titreFoncier, parcelle, dateNotif, personne);

                CiblesLigne cible = new CiblesLigne();
                NotificationOccupation notification;

                if (existante != null) {
                    notification = existante;
                    cible.notificationId = existante.getIdNotification();
                    resume.put("lignesIgnorees", (int) resume.get("lignesIgnorees") + 1);
                    log.info("Import notification — ligne {} : doublon ignoré (notification existante)", i + 1);
                } else {
                    // Création réelle (personne créée si inconnue)
                    if (personne == null) {
                        personne = creerPersonne(nomPersonne, nomAdresse, ROLE_NOTIFIE, personnes, resume);
                    } else {
                        resume.put("personnesReutilisees", (int) resume.get("personnesReutilisees") + 1);
                    }

                    notification = NotificationOccupation.builder()
                            .idNotification(UUID.randomUUID())
                            .dateNotification(dateNotif)
                            .annee(entier(row.getCell(S1_ANNEE)))
                            .dateConvocation(convocation(row.getCell(S1_CONVOCATION)))
                            .informationsOccupants(infosOccupants)
                            .constats(texte(row.getCell(S1_CONSTATS)))
                            .doleances(texte(row.getCell(S1_DOLEANCES)))
                            .actionsEntreprises(texte(row.getCell(S1_ACTIONS)))
                            .statut("En cours")
                            .titreFoncier(titreFoncier)
                            .parcelle(parcelle)
                            .build();
                    notificationRepository.save(notification);
                    cible.notificationId = notification.getIdNotification();
                    nb++;

                    if (personne != null) {
                        notificationPersonneRepository.save(NotificationPersonne.builder()
                                .idNotification(notification.getIdNotification())
                                .idPersonne(personne.getIdPersonne())
                                .roleDansNotification(ROLE_NOTIFIE)
                                .notificationOccupation(notification)
                                .personne(personne)
                                .build());
                    }
                }

                // 3) Occupant nommé dans « Informations occupants » (rôle Occupant)
                rattacherOccupant(notification, infosOccupants, personne, personnes, resume);

                // 4) Suivis (1er et 2ème suivi du fichier)
                int ordre = 1;
                for (int colDate : new int[]{S1_SUIVI1_DATE, S1_SUIVI2_DATE}) {
                    LocalDate dateSuivi = date(row.getCell(colDate));
                    if (dateSuivi == null) continue;

                    SuiviNotification suivi = null;
                    if (existante != null) {
                        // Ligne déjà importée : on récupère le suivi existant (pour ses photos)
                        suivi = suiviRepository
                                .findByNotificationOccupationIdNotificationAndOrdre(
                                        existante.getIdNotification(), ordre)
                                .orElse(null);
                    } else {
                        int colConstats = colDate + 1;
                        int colActions = colDate + 2;
                        suivi = suiviRepository.save(SuiviNotification.builder()
                                .idSuivi(UUID.randomUUID())
                                .ordre(ordre)
                                .dateSuivi(dateSuivi)
                                .constats(texte(row.getCell(colConstats)))
                                .actionsASuivre(texte(row.getCell(colActions)))
                                .notificationOccupation(notification)
                                .build());
                        resume.put("suivisCrees", (int) resume.get("suivisCrees") + 1);
                    }
                    if (suivi != null) {
                        if (ordre == 1) cible.suivi1Id = suivi.getIdSuivi();
                        else cible.suivi2Id = suivi.getIdSuivi();
                    }
                    ordre++;
                }

                cibles.put(i + 1, cible);
            } catch (Exception e) {
                log.warn("Import notification — erreur ligne {} : {}", i + 1, e.getMessage());
                erreurs.add("Ligne " + (i + 1) + " : " + e.getMessage());
            }
        }

        // 5) Photos embarquées de la feuille
        importerPhotosNotifications(feuille, cibles, resume, erreurs);

        resume.put("notificationsImportees", nb);
        log.info("Import notifications : {} nouvelles lignes importées", nb);
    }

    // ─────────────────────────────────────────────────────────────
    // Feuille 2 : avertissements (non notifiés)
    // ─────────────────────────────────────────────────────────────

    private void importerAvertissements(Sheet feuille,
                                        Map<String, Propriete> proprietes,
                                        Map<String, TitreFoncier> titres,
                                        Map<String, Parcelle> parcelles,
                                        Map<String, Personne> personnes,
                                        Map<String, Object> resume,
                                        List<String> erreurs) {
        // La feuille commence par 2 lignes d'explication puis la ligne d'en-tête
        int ligneEnTete = trouverLigneEnTete(feuille, 0, S2_TITRE);
        Map<Integer, UUID> avertissementsParLigne = new HashMap<>();
        int nb = 0;

        for (int i = ligneEnTete + 1; i <= feuille.getLastRowNum(); i++) {
            Row row = feuille.getRow(i);
            if (row == null) continue;

            String numero = texte(row.getCell(0));
            if (numero == null || numero.isEmpty()) continue; // ligne vide

            try {
                TitreFoncier titreFoncier = obtenirTitreFoncier(
                        texte(row.getCell(S2_TITRE)),
                        texte(row.getCell(S2_PROPRIETE)),
                        texte(row.getCell(S2_ZONE)),
                        texte(row.getCell(S2_LOCALISATION)),
                        proprietes, titres);
                if (titreFoncier == null) {
                    erreurs.add("Ligne " + (i + 1) + " : titre foncier manquant — ignorée");
                    continue;
                }

                Parcelle parcelle = obtenirParcelle(
                        texte(row.getCell(S2_PARCELLE)), titreFoncier, parcelles);

                String nomPersonne = texte(row.getCell(S2_PERSONNE));
                LocalDate dateAvert = date(row.getCell(S2_DATE_AVERT));

                Personne personne = trouverPersonne(nomPersonne, personnes);

                Avertissement existant = avertissementDoublon(
                        titreFoncier, parcelle, dateAvert, personne != null ? personne.getIdPersonne() : null);

                Avertissement avertissement;
                if (existant != null) {
                    avertissement = existant;
                    resume.put("lignesIgnorees", (int) resume.get("lignesIgnorees") + 1);
                    log.info("Import avertissement — ligne {} : doublon ignoré", i + 1);
                } else {
                    if (personne == null) {
                        personne = creerPersonne(nomPersonne, null, ROLE_AVERTI, personnes, resume);
                    } else {
                        resume.put("personnesReutilisees", (int) resume.get("personnesReutilisees") + 1);
                    }

                    avertissement = Avertissement.builder()
                            .idAvertissement(UUID.randomUUID())
                            .dateAvertissement(dateAvert)
                            .annee(entier(row.getCell(S2_ANNEE)))
                            .informationsOccupants(texte(row.getCell(S2_INFOS)))
                            .constats(texte(row.getCell(S2_CONSTATS)))
                            .actionsEntreprises(texte(row.getCell(S2_ACTIONS)))
                            .aFaire(texte(row.getCell(S2_A_FAIRE)))
                            .mission(texte(row.getCell(S2_MISSION)))
                            .personne(personne)
                            .titreFoncier(titreFoncier)
                            .parcelle(parcelle)
                            .build();
                    avertissementRepository.save(avertissement);
                    nb++;
                }
                avertissementsParLigne.put(i + 1, avertissement.getIdAvertissement());
            } catch (Exception e) {
                log.warn("Import avertissement — erreur ligne {} : {}", i + 1, e.getMessage());
                erreurs.add("Ligne " + (i + 1) + " : " + e.getMessage());
            }
        }

        importerPhotosAvertissements(feuille, avertissementsParLigne, resume, erreurs);

        resume.put("avertissementsImportes", nb);
        log.info("Import avertissements : {} nouvelles lignes importées", nb);
    }

    // ─────────────────────────────────────────────────────────────
    // Photos embarquées
    // ─────────────────────────────────────────────────────────────

    /** Photos de la feuille « Notifié » : colonne P (notification) et V (suivi). */
    private void importerPhotosNotifications(Sheet feuille,
                                             Map<Integer, CiblesLigne> cibles,
                                             Map<String, Object> resume,
                                             List<String> erreurs) {
        XSSFDrawing drawing = drawingDe(feuille);
        if (drawing == null) return;

        for (XSSFShape shape : drawing.getShapes()) {
            if (!(shape instanceof XSSFPicture)) continue;
            XSSFPicture picture = (XSSFPicture) shape;
            XSSFClientAnchor anc = (XSSFClientAnchor) picture.getAnchor();
            if (anc == null) continue;

            int rowExcel = anc.getRow1() + 1;   // ligne Excel (1 = en-tête)
            int colExcel = anc.getCol1() + 1;   // colonne Excel (1 = A)
            CiblesLigne cible = cibles.get(rowExcel);
            if (cible == null || cible.notificationId == null) continue;

            try {
                if (colExcel == S1_PHOTO_NOTIF + 1) {
                    sauvegarderPhoto("notification", cible.notificationId, "date_notification",
                            picture, resume);
                } else if (colExcel == S1_PHOTO_SUIVI + 1) {
                    // Photo « à la date de suivi » : rattachée à la notification avec un
                    // type dédié (visible dès aujourd'hui ; le module « suivi » pourra
                    // la re-rattacher à la ligne de suivi précise plus tard).
                    sauvegarderPhoto("notification", cible.notificationId, "photo_suivi",
                            picture, resume);
                }
            } catch (Exception e) {
                log.warn("Import photo feuille 1 — ligne {} : {}", rowExcel, e.getMessage());
                erreurs.add("Ligne " + rowExcel + " (photo) : " + e.getMessage());
            }
        }
    }

    /** Photos de la feuille « Autres Non notifié » : colonnes M / N / O. */
    private void importerPhotosAvertissements(Sheet feuille,
                                              Map<Integer, UUID> avertissementsParLigne,
                                              Map<String, Object> resume,
                                              List<String> erreurs) {
        XSSFDrawing drawing = drawingDe(feuille);
        if (drawing == null) return;

        for (XSSFShape shape : drawing.getShapes()) {
            if (!(shape instanceof XSSFPicture)) continue;
            XSSFPicture picture = (XSSFPicture) shape;
            XSSFClientAnchor anc = (XSSFClientAnchor) picture.getAnchor();
            if (anc == null) continue;

            int rowExcel = anc.getRow1() + 1;
            int colExcel = anc.getCol1() + 1;
            UUID idAvert = avertissementsParLigne.get(rowExcel);
            if (idAvert == null) continue;

            String typePhoto = switch (colExcel) {
                case S2_PHOTO_CONSTAT + 1 -> "constatation";
                case S2_PHOTO_SAT_APRES + 1 -> "satellite_apres";
                case S2_PHOTO_SAT_AVANT + 1 -> "satellite_avant";
                default -> null;
            };
            if (typePhoto == null) continue;

            try {
                sauvegarderPhoto("avertissement", idAvert, typePhoto, picture, resume);
            } catch (Exception e) {
                log.warn("Import photo feuille 2 — ligne {} : {}", rowExcel, e.getMessage());
                erreurs.add("Ligne " + rowExcel + " (photo) : " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private XSSFDrawing drawingDe(Sheet feuille) {
        try {
            Object patriarch = feuille.getDrawingPatriarch();
            return patriarch instanceof XSSFDrawing ? (XSSFDrawing) patriarch : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Écrit le fichier image sur disque et enregistre la ligne photo (une seule par type). */
    private void sauvegarderPhoto(String entiteType, UUID entiteId, String typePhoto,
                                  XSSFPicture picture, Map<String, Object> resume) throws IOException {
        XSSFPictureData donnees = picture.getPictureData();
        if (donnees == null || donnees.getData() == null || donnees.getData().length == 0) return;

        if (photoRepository.findByEntiteTypeAndEntiteIdAndTypePhoto(entiteType, entiteId, typePhoto).isPresent()) {
            log.info("Photo {} déjà importée pour {}/{}", typePhoto, entiteType, entiteId);
            return;
        }

        String ext = donnees.suggestFileExtension();
        if (ext == null || ext.isEmpty()) ext = "jpg";
        ext = ext.toLowerCase();

        UUID idPhoto = UUID.randomUUID();
        String nomFichier = idPhoto + "." + ext;

        Path racine = Paths.get(stockagePhotos).toAbsolutePath();
        Files.createDirectories(racine);
        Files.write(racine.resolve(nomFichier), donnees.getData());

        photoRepository.save(Photo.builder()
                .idPhoto(idPhoto)
                .entiteType(entiteType)
                .entiteId(entiteId)
                .typePhoto(typePhoto)
                .cheminFichier(nomFichier)
                .build());
        resume.put("photosImportees", (int) resume.get("photosImportees") + 1);
        log.info("Photo importée : {}/{} ({}, {} octets)", entiteType, entiteId, typePhoto, donnees.getData().length);
    }

    // ─────────────────────────────────────────────────────────────
    // Référentiels & personnes (avec cache)
    // ─────────────────────────────────────────────────────────────

    private TitreFoncier obtenirTitreFoncier(String numero,
                                             String nomPropriete,
                                             String zone,
                                             String localisation,
                                             Map<String, Propriete> proprietes,
                                             Map<String, TitreFoncier> titres) {
        String numeroClean = nettoyer(numero);
        if (numeroClean == null) return null;

        TitreFoncier titre = titres.get(numeroClean);
        if (titre == null) {
            titre = titreFoncierRepository.findByNumero(numeroClean).orElse(null);
        }
        if (titre == null) {
            Propriete propriete = obtenirPropriete(nomPropriete, proprietes);
            titre = TitreFoncier.builder()
                    .idTitreFoncier(UUID.randomUUID())
                    .numero(numeroClean)
                    .zone(nettoyer(zone))
                    .localisation(nettoyer(localisation))
                    .propriete(propriete)
                    .build();
            titreFoncierRepository.save(titre);
            titres.put(numeroClean, titre);
            return titre;
        }
        titres.put(numeroClean, titre);
        return titre;
    }

    private Propriete obtenirPropriete(String nom, Map<String, Propriete> proprietes) {
        String nomClean = nettoyer(nom);
        if (nomClean == null || nomClean.isEmpty()) return null;

        Propriete propriete = proprietes.get(nomClean.toLowerCase());
        if (propriete == null) {
            propriete = proprieteRepository.findByNomIgnoreCase(nomClean).orElse(null);
        }
        if (propriete == null) {
            propriete = proprieteRepository.save(Propriete.builder().nom(nomClean).build());
        }
        proprietes.put(nomClean.toLowerCase(), propriete);
        return propriete;
    }

    private Parcelle obtenirParcelle(String numeroLot,
                                     TitreFoncier titreFoncier,
                                     Map<String, Parcelle> parcelles) {
        String lotClean = nettoyer(numeroLot);
        if (lotClean == null || lotClean.isEmpty() || titreFoncier == null) return null;

        String cle = titreFoncier.getIdTitreFoncier() + "|" + lotClean;
        Parcelle parcelle = parcelles.get(cle);
        if (parcelle == null) {
            parcelle = parcelleRepository
                    .findByNumeroLotAndTitreFoncierIdTitreFoncier(lotClean, titreFoncier.getIdTitreFoncier())
                    .orElse(null);
        }
        if (parcelle == null) {
            parcelle = Parcelle.builder()
                    .idParcelle(UUID.randomUUID())
                    .numeroLot(lotClean)
                    .titreFoncier(titreFoncier)
                    .build();
            parcelleRepository.save(parcelle);
        }
        parcelles.put(cle, parcelle);
        return parcelle;
    }

    /** Recherche d'une personne en cache puis en base (sans création). */
    private Personne trouverPersonne(String nom, Map<String, Personne> personnes) {
        String nomClean = nettoyer(nom);
        if (nomClean == null || nomClean.isEmpty()) return null;

        Personne personne = personnes.get(nomClean.toLowerCase());
        if (personne == null) {
            personne = personneRepository.findByNomIgnoreCase(nomClean).orElse(null);
            if (personne != null) personnes.put(nomClean.toLowerCase(), personne);
        }
        return personne;
    }

    /** Recherche ou création d'une personne (avec compteurs). */
    private Personne creerPersonne(String nom, String adresse, String role,
                                   Map<String, Personne> personnes, Map<String, Object> resume) {
        String nomClean = nettoyer(nom);
        if (nomClean == null || nomClean.isEmpty()) return null;

        Personne personne = trouverPersonne(nomClean, personnes);
        if (personne == null) {
            personne = Personne.builder()
                    .nom(nomClean)
                    .adresse(nettoyer(adresse))
                    .role(role)
                    .typePersonne(typePourRole(role))
                    .build();
            personneRepository.save(personne);
            personnes.put(nomClean.toLowerCase(), personne);
            resume.put("personnesCrees", (int) resume.get("personnesCrees") + 1);
        }
        return personne;
    }

    /** Type de personne correspondant au rôle libre (Notifié / Averti / Occupant…). */
    private com.seimad.patrimoine.entity.notification.TypePersonne typePourRole(String role) {
        if (role == null || role.isBlank()) return null;
        String r = role.toLowerCase();
        String code;
        if (r.contains("notif")) code = "NOTIFIE";
        else if (r.contains("avert")) code = "AVERTI";
        else if (r.contains("occup")) code = "OCCUPANT";
        else if (r.contains("propri")) code = "PROPRIETAIRE";
        else if (r.contains("mandat")) code = "MANDATAIRE";
        else code = "DEMANDEUR";
        return typePersonneRepository.findByCode(code).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────
    // Détection de doublons (import idempotent)
    // ─────────────────────────────────────────────────────────────

    private NotificationOccupation notificationDoublon(TitreFoncier titre, Parcelle parcelle,
                                                       LocalDate date, Personne personne) {
        if (titre == null || date == null) return null;
        for (NotificationOccupation n : notificationRepository.trouverDoublons(
                titre.getIdTitreFoncier(),
                parcelle != null ? parcelle.getIdParcelle() : null,
                date)) {
            if (personne == null) return n;
            boolean memePersonne = notificationPersonneRepository
                    .findByIdNotification(n.getIdNotification()).stream()
                    .anyMatch(np -> personne.getIdPersonne().equals(np.getIdPersonne()));
            if (memePersonne) return n;
        }
        return null;
    }

    private Avertissement avertissementDoublon(TitreFoncier titre, Parcelle parcelle,
                                               LocalDate date, Integer idPersonne) {
        if (titre == null || date == null) return null;
        List<Avertissement> doublons = avertissementRepository.trouverDoublons(
                titre.getIdTitreFoncier(),
                parcelle != null ? parcelle.getIdParcelle() : null,
                date,
                idPersonne);
        return doublons.isEmpty() ? null : doublons.get(0);
    }

    // ─────────────────────────────────────────────────────────────
    // Occupants nommés dans « Informations occupants »
    // ─────────────────────────────────────────────────────────────

    private static final Pattern OCC_MARQUE = Pattern.compile(
            "(?i)(propri[ée]taire|occupant|g[ée]rant)\\s*:\\s*([^0-9:;]{2,80}?)(?=\\s*(contact|t[eé]l(?:[eé]phone)?\\s*:?|\\d)|[:;]|$)");
    private static final Pattern OCC_CIVILITE = Pattern.compile(
            "^(?:m(?:r|me|lle)?\\.?|madame|monsieur)\\s+([^0-9]{3,80}?)(?=\\s*(contact|t[eé]l(?:[eé]phone)?\\s*:?|\\d)|[:;]|$)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Extrait un nom d'occupant « lisible » depuis la colonne Informations occupants.
     * Reste volontairement prudent : beaucoup de cellules contiennent seulement un
     * téléphone ou une phrase libre — dans ce cas on ne crée personne.
     */
    private String extraireNomOccupant(String texte) {
        if (texte == null) return null;
        String v = texte.replaceAll("\\s+", " ").trim();
        if (v.isEmpty()) return null;

        Matcher m = OCC_MARQUE.matcher(v);
        String candidat = null;
        if (m.find()) candidat = m.group(2);
        if (candidat == null || candidat.trim().isEmpty()) {
            Matcher c = OCC_CIVILITE.matcher(v);
            if (c.find()) candidat = c.group(1);
        }
        if (candidat == null) return null;

        // Garde les 3 premiers mots max (nom + prénom(s)), sans chiffres superflus
        String[] mots = candidat.trim().replaceAll("[.,;:]+$", "").split("\\s+");
        StringBuilder nom = new StringBuilder();
        for (String mot : mots) {
            if (nom.length() > 0 && nom.length() + mot.length() > 45) break;
            if (nom.length() > 0 && Character.isDigit(mot.charAt(0))) break;
            if (nom.length() > 0) nom.append(' ');
            nom.append(mot);
            if (nom.toString().split("\\s+").length >= 3) break;
        }
        String resultat = nom.toString().trim().replaceAll("\\s+", " ");
        // Exclusions : texte trop court, pur téléphone, ou marqueurs seuls
        if (resultat.length() < 5) return null;
        int chiffres = (int) resultat.chars().filter(Character::isDigit).count();
        if (chiffres * 2 > resultat.length()) return null;
        if (!resultat.matches(".*[A-ZÀ-ÖØ-Þ].*")) return null;
        return resultat;
    }

    /** Crée/relie la Personne « Occupant » quand le texte contient un nom exploitable. */
    private void rattacherOccupant(NotificationOccupation notification, String infosOccupants,
                                   Personne personneNotifiee, Map<String, Personne> personnes,
                                   Map<String, Object> resume) {
        String nomOccupant = extraireNomOccupant(infosOccupants);
        if (nomOccupant == null) return;

        // Ne pas créer de doublon quand l'occupant est la personne notifiée elle-même
        if (personneNotifiee != null && memesNoms(personneNotifiee.getNom(), nomOccupant)) return;

        Personne occupant = creerPersonne(nomOccupant, null, ROLE_OCCUPANT, personnes, resume);
        if (occupant == null) return;

        boolean dejaLien = notificationPersonneRepository.findByIdNotification(notification.getIdNotification())
                .stream()
                .anyMatch(np -> np.getIdPersonne().equals(occupant.getIdPersonne()));
        if (dejaLien) return;

        notificationPersonneRepository.save(NotificationPersonne.builder()
                .idNotification(notification.getIdNotification())
                .idPersonne(occupant.getIdPersonne())
                .roleDansNotification(ROLE_OCCUPANT)
                .notificationOccupation(notification)
                .personne(occupant)
                .build());
        log.info("Occupant « {} » lié à la notification {}", occupant.getNom(), notification.getIdNotification());
    }

    private boolean memesNoms(String a, String b) {
        if (a == null || b == null) return false;
        return a.replaceAll("[\\s.'-]+", "").equalsIgnoreCase(b.replaceAll("[\\s.'-]+", ""));
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers de lecture de cellules
    // ─────────────────────────────────────────────────────────────

    /** Recherche la ligne d'en-tête (colonne contenant « Titre ») à partir de départ. */
    private int trouverLigneEnTete(Sheet feuille, int depart, int colTitre) {
        for (int i = depart; i <= Math.min(feuille.getLastRowNum(), depart + 10); i++) {
            Row row = feuille.getRow(i);
            if (row == null) continue;
            String valeur = texte(row.getCell(colTitre));
            if (valeur != null && valeur.toLowerCase().contains("titre")) {
                return i;
            }
        }
        return depart;
    }

    /** Texte d'une cellule (nombre → chaîne, date → chaîne formatée). */
    private String texte(Cell cell) {
        if (cell == null) return null;
        String valeur = FORMATTER.formatCellValue(cell);
        if (valeur == null || valeur.trim().isEmpty()) return null;
        return valeur.trim();
    }

    /** Date d'une cellule (date formatée ou texte parsable). */
    private LocalDate date(Cell cell) {
        if (cell == null) return null;
        // Uniquement pour les cellules numériques : une cellule texte avec un
        // format de date (ex. ligne d'en-tête) ferait lever une exception POI
        if (DateUtil.isCellDateFormatted(cell) && cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            // Sérial Excel : nombre de jours depuis 1899-12-30
            // (borné pour éviter d'interpréter une année comme une date)
            double serial = cell.getNumericCellValue();
            if (serial >= 20000 && serial <= 60000) {
                return DateUtil.getLocalDateTime(serial).toLocalDate();
            }
            return null;
        }
        String valeur = texte(cell);
        if (valeur == null) return null;
        for (String pattern : new String[]{"yyyy-MM-dd", "dd/MM/yyyy", "dd/MM/yy"}) {
            try {
                return java.time.LocalDate.parse(valeur, java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // essayer le format suivant
            }
        }
        return null;
    }

    /** Entier d'une cellule (année). */
    private Integer entier(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String valeur = texte(cell);
        if (valeur == null) return null;
        try {
            return (int) Double.parseDouble(valeur);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** « Date et heure de convocation » : texte libre type « 24/10/23 9h ». */
    private LocalDateTime convocation(Cell cell) {
        String valeur = texte(cell);
        if (valeur == null) return null;

        Matcher m = CONVOCATION_DATE.matcher(valeur);
        if (!m.find()) return null;
        int jour = Integer.parseInt(m.group(1));
        int mois = Integer.parseInt(m.group(2));
        int annee = Integer.parseInt(m.group(3));
        if (annee < 100) annee += 2000;

        Matcher hm = CONVOCATION_HEURE.matcher(valeur);
        int heure = hm.find() ? Integer.parseInt(hm.group(1)) : 0;

        try {
            return LocalDateTime.of(annee, mois, jour, Math.min(heure, 23), 0);
        } catch (Exception e) {
            return null;
        }
    }

    /** Normalise un texte : trim + suppression des sauts de ligne superflus. */
    private String nettoyer(String valeur) {
        if (valeur == null) return null;
        String v = valeur.trim();
        if (v.isEmpty()) return null;
        // Remplace les suites d'espaces/sauts de ligne par un seul espace
        return v.replaceAll("\\s+", " ").trim();
    }
}
