package com.seimad.patrimoine.service.dossier;

import com.seimad.patrimoine.entity.auth.Utilisateur;
import com.seimad.patrimoine.entity.dossier.Dossier;
import com.seimad.patrimoine.entity.dossier.DossierDemandeur;
import com.seimad.patrimoine.entity.dossier.DossierFournie;
import com.seimad.patrimoine.entity.dossier.DossierParcelle;
import com.seimad.patrimoine.entity.dossier.StatutDossier;
import com.seimad.patrimoine.entity.notification.Personne;
import com.seimad.patrimoine.entity.referentiel.Parcelle;
import com.seimad.patrimoine.entity.referentiel.Propriete;
import com.seimad.patrimoine.entity.referentiel.Lieu;
import com.seimad.patrimoine.entity.referentiel.Ville;
import com.seimad.patrimoine.repository.carte.GeometrieRepository;
import com.seimad.patrimoine.repository.dossier.DossierDemandeurRepository;
import com.seimad.patrimoine.repository.dossier.DossierParcelleRepository;
import com.seimad.patrimoine.repository.dossier.DossierRepository;
import com.seimad.patrimoine.repository.dossier.DossierFournieRepository;
import com.seimad.patrimoine.repository.dossier.StatutDossierRepository;
import com.seimad.patrimoine.repository.notification.PersonneRepository;
import com.seimad.patrimoine.repository.referentiel.PrixM2Repository;
import com.seimad.patrimoine.repository.referentiel.ProprieteRepository;
import com.seimad.patrimoine.repository.referentiel.LieuRepository;
import com.seimad.patrimoine.repository.referentiel.VilleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Import du fichier Excel « Situation des demandes Patrimoines ».
 * <p>
 * Structure attendue du classeur :
 * <ul>
 *   <li>une feuille « Prix au m² » : ville | prix TTC/m² | observation | HT —
 *       importée dans {@code prix_m2} (historique conservé) ;</li>
 *   <li>une feuille par ville : les colonnes sont <b>détectées via la ligne
 *       d'en-têtes</b> (N° Dossier, Date, Année, Demandeur, Contact, Propriété,
 *       TN°, Lieu, Coordonnée X/Y, Superficie, Valeur TTC, Observation,
 *       Dossier fournie — l'ordre et la présence de certaines colonnes
 *       peuvent varier d'une feuille à l'autre) ; chaque ligne crée (ou met
 *       à jour) un dossier avec son demandeur, sa propriété et sa parcelle ;</li>
 *   <li>le <b>numéro de dossier</b> est repris tel quel (sans préfixe), seule
 *       la <b>date</b> est importée (pas l'Année), l'<b>observation</b> alimente
 *       {@code dossier.observation}, la colonne <b>Dossier fournie</b> est
 *       rattachée via le référentiel {@code dossier_fournie} (créé au besoin)
 *       et le <b>Lieu</b> est rattaché à la <b>province</b> de la feuille dans
 *       {@code lieu} ;</li>
 *   <li>les <b>coordonnées X/Y Laborde</b> sont enregistrées dans la table
 *       {@code geometrie} rattachée à la propriété (entite_type = « propriete »),
 *       converties en WGS84 côté PostGIS (ST_Transform depuis EPSG:29701) ;</li>
 *   <li>les feuilles de structure différente (ex. « Ap SEIMAD BONHEUR »,
 *       « MORONDAVA ») sont ignorées et signalées dans le résumé.</li>
 * </ul>
 * L'import est <b>idempotent</b> : un dossier est reconnu par son numéro
 * normalisé (« IMP-ville-n° »). Il est <b>résilient</b> : chaque ligne Excel
 * est traitée dans sa propre transaction ; une ligne en erreur ne bloque
 * pas les suivantes (l'erreur est listée dans le résumé).
 */
@Service
@Slf4j
public class SituationDemandesImportService {

    private final VilleRepository villeRepository;
    private final PrixM2Repository prixM2Repository;
    private final DossierRepository dossierRepository;
    private final DossierFournieRepository dossierFournieRepository;
    private final StatutDossierRepository statutDossierRepository;
    private final PersonneRepository personneRepository;
    private final ProprieteRepository proprieteRepository;
    private final LieuRepository lieuRepository;
    private final com.seimad.patrimoine.repository.referentiel.ParcelleRepository parcelleRepository;
    private final DossierParcelleRepository dossierParcelleRepository;
    private final DossierDemandeurRepository dossierDemandeurRepository;
    private final GeometrieRepository geometrieRepository;
    private final com.seimad.patrimoine.repository.auth.UtilisateurRepository utilisateurRepository;
    private final TransactionTemplate transactionTemplate;

    private static final DataFormatter FORMATTER = new DataFormatter();

    /** Feuilles de structure différente : ignorées mais signalées dans le résumé. */
    private static final List<String> FEUILLES_IGNOREES = List.of(
            "Ap SEIMAD BONHEUR Mahajanga", "MORONDAVA");

    public SituationDemandesImportService(
            VilleRepository villeRepository,
            PrixM2Repository prixM2Repository,
            DossierRepository dossierRepository,
            DossierFournieRepository dossierFournieRepository,
            StatutDossierRepository statutDossierRepository,
            PersonneRepository personneRepository,
            ProprieteRepository proprieteRepository,
            LieuRepository lieuRepository,
            com.seimad.patrimoine.repository.referentiel.ParcelleRepository parcelleRepository,
            DossierParcelleRepository dossierParcelleRepository,
            DossierDemandeurRepository dossierDemandeurRepository,
            GeometrieRepository geometrieRepository,
            com.seimad.patrimoine.repository.auth.UtilisateurRepository utilisateurRepository,
            PlatformTransactionManager transactionManager) {
        this.villeRepository = villeRepository;
        this.prixM2Repository = prixM2Repository;
        this.dossierRepository = dossierRepository;
        this.dossierFournieRepository = dossierFournieRepository;
        this.statutDossierRepository = statutDossierRepository;
        this.personneRepository = personneRepository;
        this.proprieteRepository = proprieteRepository;
        this.lieuRepository = lieuRepository;
        this.parcelleRepository = parcelleRepository;
        this.dossierParcelleRepository = dossierParcelleRepository;
        this.dossierDemandeurRepository = dossierDemandeurRepository;
        this.geometrieRepository = geometrieRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Contexte d'exécution d'un import (compteurs + erreurs). */
    private static class Ctx {
        final List<String> feuillesTraitees = new ArrayList<>();
        final List<String> erreurs = new ArrayList<>();
        /** SRID Laborde (29701) connu de PostGIS ? Sinon les coordonnées sont ignorées. */
        boolean sridLabordeOk;
        int prixImportes;
        int dossiersImportes;
        int dossiersMisAJour;
        int parcellesLiees;
        int lieuxImportes;
        int dossiersFournisImportes;
        int geometriesCreees;
        /** Cache (idVille, libellé minuscule) → Lieu, pour éviter les requêtes répétées. */
        final Map<String, Lieu> lieuxCache = new HashMap<>();
        int personnesReutilisees;
        int proprietesReutilisees;
    }

    // NOTE : PAS de @Transactional ici — chaque ligne est isolée dans sa propre
    // transaction (TransactionTemplate) pour qu'une erreur n'empoisonne pas
    // la session Hibernate des lignes suivantes (HHH000099).
    public Map<String, Object> importer(MultipartFile fichier) throws IOException {
        Ctx ctx = new Ctx();
        Utilisateur utilisateur = utilisateurConnecte();
        try {
            ctx.sridLabordeOk = geometrieRepository.sridLabordeDisponible();
        } catch (Exception e) {
            ctx.sridLabordeOk = false;
        }
        if (!ctx.sridLabordeOk) {
            ctx.erreurs.add("SRID Laborde (EPSG:29701) indisponible dans PostGIS : "
                    + "les coordonnées X/Y ne seront pas importées.");
        }

        try (InputStream in = fichier.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            for (Sheet sheet : wb) {
                String nom = normaliser(sheet.getSheetName());
                if (estFeuillePrix(nom)) {
                    ctx.feuillesTraitees.add(nom);
                    importerPrix(sheet, ctx);
                } else if (FEUILLES_IGNOREES.stream().map(this::normaliser).anyMatch(nom::equals)) {
                    ctx.erreurs.add("Feuille ignorée (structure différente) : " + nom);
                } else {
                    importerVille(sheet, ctx, utilisateur);
                }
            }
        } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException e) {
            throw new IOException("Le fichier doit être un classeur Excel .xlsx", e);
        }

        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("feuillesTraitees", ctx.feuillesTraitees);
        resume.put("prixImportes", ctx.prixImportes);
        resume.put("dossiersImportes", ctx.dossiersImportes);
        resume.put("dossiersMisAJour", ctx.dossiersMisAJour);
        resume.put("parcellesLiees", ctx.parcellesLiees);
        resume.put("lieuxImportes", ctx.lieuxImportes);
        resume.put("dossiersFournisImportes", ctx.dossiersFournisImportes);
        resume.put("geometriesCreees", ctx.geometriesCreees);
        resume.put("personnesReutilisees", ctx.personnesReutilisees);
        resume.put("proprietesReutilisees", ctx.proprietesReutilisees);
        resume.put("erreurs", ctx.erreurs);
        return resume;
    }

    // ════════════════════════════════════════════════════════════
    // Feuille « Prix au m² » — colonnes fixes A/B/C/D
    // ════════════════════════════════════════════════════════════

    private void importerPrix(Sheet sheet, Ctx ctx) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String nomVille = texteCellule(row.getCell(0));
            BigDecimal ttc = nombre(row, 1);
            if (nomVille.isBlank() || ttc == null) continue;

            try {
                Boolean ok = transactionTemplate.execute(tx -> {
                    importerLignePrix(row, nomVille, ttc, ctx);
                    return true;
                });
            } catch (Exception e) {
                String msg = "Prix ligne " + (i + 1) + " (" + nomVille + ") : " + e.getMessage();
                ctx.erreurs.add(msg);
                log.warn("Import prix : {}", msg);
            }
        }
    }

    /** Exécuté dans la transaction de la ligne (voir importerPrix). */
    private void importerLignePrix(Row row, String nomVille, BigDecimal ttc, Ctx ctx) {
        Ville ville = trouverOuCreerVille(tronquer(nomVille, 100));
        BigDecimal ht = nombre(row, 3);
        BigDecimal ttcScale = ttc.setScale(2, RoundingMode.HALF_UP);

        // Historique conservé : insère seulement si le dernier prix connu diffère
        List<com.seimad.patrimoine.entity.referentiel.PrixM2> existants =
                prixM2Repository.findByVilleIdVilleOrderByDateMajDesc(ville.getIdVille());
        if (!existants.isEmpty()) {
            com.seimad.patrimoine.entity.referentiel.PrixM2 dernier = existants.get(0);
            boolean identique = ttcScale.compareTo(dernier.getPrixTtc()) == 0
                    && ((ht == null && dernier.getPrixHt() == null)
                        || (ht != null && ht.compareTo(dernier.getPrixHt()) == 0));
            if (identique) return;
        }

        prixM2Repository.save(
                com.seimad.patrimoine.entity.referentiel.PrixM2.builder()
                        .prixTtc(ttcScale)
                        .prixHt(ht)
                        .observation(tronquer(texteCellule(row.getCell(2)), 255))
                        .dateMaj(LocalDate.now())
                        .ville(ville)
                        .build());
        ctx.prixImportes++;
    }

    // ════════════════════════════════════════════════════════════
    // Feuilles villes
    // ════════════════════════════════════════════════════════════

    private void importerVille(Sheet sheet, Ctx ctx, Utilisateur utilisateur) {
        String nomFeuille = sheet.getSheetName().trim();

        // ── Mapping des colonnes à partir de la ligne d'en-têtes ──
        Map<String, Integer> colonnes = mapperColonnes(sheet.getRow(0));
        if (colonnes == null || !colonnes.containsKey("dossier")) {
            ctx.erreurs.add("Feuille ignorée (en-têtes non reconnus) : " + nomFeuille);
            return;
        }

        Ville ville = villeRepository.findByNomVilleIgnoreCase(tronquer(nomFeuille, 100))
                .orElseGet(() -> {
                    log.info("Ville inconnue « {} » : créée à l'import", nomFeuille);
                    return villeRepository.save(Ville.builder()
                            .nomVille(tronquer(nomFeuille, 100)).build());
                });

        StatutDossier statutDefaut = statutDefaut();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String numero = texteCellule(row.getCell(colonnes.get("dossier"))).trim();
            if (numero.isBlank() || "0".equals(numero)) continue;

            // ── Chaque ligne = une transaction isolée ──
            try {
                transactionTemplate.executeWithoutResult(tx ->
                        traiterLigne(row, colonnes, numero, ville, statutDefaut, utilisateur, ctx));
            } catch (Exception e) {
                String msg = ville.getNomVille() + " ligne " + (i + 1)
                        + " (dossier " + numero + ") : " + messageRacine(e);
                ctx.erreurs.add(msg);
                log.warn("Import dossier : {}", msg);
            }
        }
        ctx.feuillesTraitees.add(nomFeuille);
    }

    /** Exécuté dans la transaction de la ligne (voir importerVille). */
    private void traiterLigne(Row row, Map<String, Integer> c, String numero, Ville ville,
                              StatutDossier statutDefaut, Utilisateur utilisateur, Ctx ctx) {
        String demandeurNom = champ(row, c, "demandeur");
        String contact = champ(row, c, "contact");
        String nomPropriete = champ(row, c, "propriete");
        String tn = champ(row, c, "tn");
        // Lieu brut (les retours à la ligne du fichier deviennent « / » comme dans le référentiel)
        String lieu = normaliserLieu(c.containsKey("lieu")
                ? texteCellule(row.getCell(c.get("lieu"))) : "");
        BigDecimal superficie = c.containsKey("superficie") ? nombre(row, c.get("superficie")) : null;
        BigDecimal valeurTtc = c.containsKey("valeur") ? nombre(row, c.get("valeur")) : null;
        BigDecimal coordX = c.containsKey("x") ? nombre(row, c.get("x")) : null;
        BigDecimal coordY = c.containsKey("y") ? nombre(row, c.get("y")) : null;
        LocalDate date = c.containsKey("date") ? date(row, c.get("date")) : null;
        String observation = c.containsKey("observation")
                ? texteCellule(row.getCell(c.get("observation"))) : "";
        String dossierFournie = champ(row, c, "fournie");

        // ── Idempotence : dossier déjà importé ? ──
        String numeroRecherche = numeroImporte(numero);
        Dossier dossier = dossierRepository.findFirstByNumeroDossierOrderByDateCreationDesc(numeroRecherche).orElse(null);
        boolean creation = (dossier == null);
        if (creation) {
            dossier = Dossier.builder()
                    .numeroDossier(numeroRecherche)
                    .dateDemande(date)
                    .observation(observation.isBlank() ? null : observation)
                    .montantTotal(BigDecimal.ZERO)
                    .statutDossier(statutDefaut)
                    .ville(ville)
                    .build();
            dossier = dossierRepository.save(dossier);
        } else {
            // Complète les champs manquants lors d'un ré-import
            if (dossier.getDateDemande() == null) dossier.setDateDemande(date);
        }

        // ── Demandeur ──
        if (!demandeurNom.isBlank()) {
            Personne personne = personneRepository.findFirstByNomIgnoreCase(demandeurNom)
                    .orElseGet(() -> personneRepository.save(Personne.builder()
                            .nom(tronquer(demandeurNom, 200))
                            .contact(contact.isBlank() ? null : tronquer(contact, 100))
                            .role("Demandeur")
                            .build()));
            if (!dossierDemandeurRepository.existsByIdDossierAndIdPersonne(
                    dossier.getIdDossier(), personne.getIdPersonne())) {
                dossierDemandeurRepository.save(DossierDemandeur.builder()
                        .idDossier(dossier.getIdDossier())
                        .idPersonne(personne.getIdPersonne())
                        .role("Principal")
                        .build());
            } else {
                ctx.personnesReutilisees++;
            }
        }

        // ── Dossier fournie (référentiel dossier_fournie, créé au besoin) ──
        if (!dossierFournie.isBlank()) {
            dossier.setDossierFournie(dossierFournieRepository.findByLibelleIgnoreCase(dossierFournie)
                    .orElseGet(() -> dossierFournieRepository.save(
                            DossierFournie.builder().libelle(tronquer(dossierFournie, 100)).build())));
            ctx.dossiersFournisImportes++;
        }

        // ── Lieu : colonne « Lieu » de la feuille, rattaché à la province ──
        Lieu lieuEntite = lieu.isBlank() ? null : lieuParOuCreer(ville, normaliserLieu(lieu), ctx);

        // ── Propriété (nom + TN°) ──
        if (!nomPropriete.isBlank()) {
                Lieu lieuPropriete = (lieuEntite != null) ? lieuEntite
                        : lieuParOuCreer(ville, ville.getNomVille(), ctx);
                Propriete propriete = proprieteRepository.findFirstByNomIgnoreCase(nomPropriete)
                    .orElseGet(() -> proprieteRepository.save(Propriete.builder()
                            .nom(tronquer(nomPropriete, 200))
                            .numero(tn.isBlank() ? null : tronquer(tn, 50))
                            .localisation(lieu.isBlank() ? null : lieu)
                            .lieu(lieuPropriete)
                            .build()));

            // ── Parcelle (N° de lot si présent dans le fichier, ex. feuille Anosiala) ──
            String numeroLot = champ(row, c, "lot");
            Parcelle parcelle = null;
            if (!numeroLot.isBlank() && propriete.getIdPropriete() != null) {
                parcelle = parcelleRepository.findByNumeroLotAndProprieteIdPropriete(
                                numeroLot, propriete.getIdPropriete())
                        .orElseGet(() -> parcelleRepository.save(Parcelle.builder()
                                .idParcelle(UUID.randomUUID())
                                .numeroLot(tronquer(numeroLot, 50))
                                .propriete(propriete)
                                .build()));
            }

            // ── Parcelle demandée (superficie de la ligne) ──
            List<DossierParcelle> existantes = dossierParcelleRepository
                    .findByDossier_IdDossierOrderByDateAjoutAsc(dossier.getIdDossier());
            DossierParcelle dp = existantes.isEmpty() ? null : existantes.get(0);
            if (dp == null) {
                dossierParcelleRepository.save(DossierParcelle.builder()
                        .idDossierParcelle(UUID.randomUUID())
                        .dossier(dossier)
                        .propriete(propriete)
                        .parcelle(parcelle)
                        .superficieM2(superficie)
                        .observation("Import Excel" + (lieuEntite != null ? " — " + lieuEntite.getLibelle() : ""))
                        .build());
                ctx.parcellesLiees++;
            } else if ((parcelle != null && !parcelle.equals(dp.getParcelle()))
                    || (superficie != null && (dp.getSuperficieM2() == null
                        || dp.getSuperficieM2().compareTo(superficie) != 0))) {
                if (parcelle != null) dp.setParcelle(parcelle);
                if (superficie != null) dp.setSuperficieM2(superficie);
                dossierParcelleRepository.save(dp);
            }

            // ── Coordonnées X/Y → geometrie (parcelle si lot, sinon propriété) ──
            if (coordX != null && coordY != null && ctx.sridLabordeOk && utilisateur != null) {
                String typeEntite;
                UUID idEntite;
                if (parcelle != null) {
                    typeEntite = "parcelle";
                    idEntite = parcelle.getIdParcelle();
                } else if (propriete.getIdPropriete() != null) {
                    typeEntite = "propriete";
                    idEntite = integerToUuid(propriete.getIdPropriete());
                } else {
                    typeEntite = null;
                    idEntite = null;
                }
                if (typeEntite != null) {
                    try {
                        int n = geometrieRepository.upsertPointLaborde(
                                typeEntite,
                                idEntite,
                                coordX.doubleValue(),
                                coordY.doubleValue(),
                                "Import Excel",
                                utilisateur.getIdUtilisateur());
                        if (n > 0) ctx.geometriesCreees++;
                    } catch (Exception e) {
                        ctx.erreurs.add("Coordonnées X/Y non importées (" + typeEntite + " "
                                + idEntite + ") : " + messageRacine(e));
                    }
                }
            }

            // ── Montant total = valeur TTC de la ligne (fichier de référence) ──
            if (valeurTtc != null) {
                BigDecimal ttc = valeurTtc.setScale(2, RoundingMode.HALF_UP);
                if (ttc.signum() > 0 && ttc.compareTo(dossier.getMontantTotal()) != 0) {
                    dossier.setMontantTotal(ttc);
                }
            }
        }

        dossier.setDateModification(LocalDateTime.now());
        dossierRepository.save(dossier);
        if (creation) ctx.dossiersImportes++;
        else ctx.dossiersMisAJour++;
    }

    // ════════════════════════════════════════════════════════════
    // Mapping des colonnes par en-têtes
    // ════════════════════════════════════════════════════════════

    /**
     * Reconnaît les colonnes via leur libellé (les feuilles villes ont des
     * layouts variables : « N° Lot » en tête sur Anosiala, absence du « Lieu »
     * sur Fianarantsoa/Manakara/Antsiranana, etc.). Clés renvoyées :
     * dossier, date, annee, demandeur, contact, propriete, tn, lieu, x, y,
     * superficie, valeur, observation, fournie.
     */
    private Map<String, Integer> mapperColonnes(Row header) {
        if (header == null) return null;
        Map<String, Integer> m = new HashMap<>();
        for (int idx = 0; idx < header.getLastCellNum(); idx++) {
            String brut = texteCellule(header.getCell(idx));
            if (brut.isBlank()) continue;
            String h = nettoyerEntete(brut);

            if (h.contains("manquant") || h.contains("ref")) continue;
            if (h.contains("lot") && !h.contains("dossier") && !m.containsKey("lot")) m.put("lot", idx);
            if (h.contains("dossier") && !m.containsKey("dossier")) m.put("dossier", idx);
            else if (h.contains("date") && !m.containsKey("date")) m.put("date", idx);
            else if (h.contains("annee") && !m.containsKey("annee")) m.put("annee", idx);
            else if (h.contains("demandeur") && !m.containsKey("demandeur")) m.put("demandeur", idx);
            else if (h.contains("contact") && !m.containsKey("contact")) m.put("contact", idx);
            else if (h.contains("propriete") && !m.containsKey("propriete")) m.put("propriete", idx);
            else if (h.equals("tn") && !m.containsKey("tn")) m.put("tn", idx);
            else if (h.contains("lieu") && !m.containsKey("lieu")) m.put("lieu", idx);
            else if (h.contains("coordonnee") && h.endsWith("x") && !m.containsKey("x")) m.put("x", idx);
            else if (h.contains("coordonnee") && h.endsWith("y") && !m.containsKey("y")) m.put("y", idx);
            else if (h.contains("superficie") && !m.containsKey("superficie")) m.put("superficie", idx);
            else if ((h.contains("valeur") || h.contains("montant")) && !m.containsKey("valeur"))
                m.put("valeur", idx);
            else if (h.contains("observation") && !m.containsKey("observation")) m.put("observation", idx);
            else if (h.contains("fournie") && !m.containsKey("fournie")) m.put("fournie", idx);
        }
        return m;
    }

    /** Minuscule, accents retirés, ne garde que lettres et chiffres. */
    private String nettoyerEntete(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    // ════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════

    /** Numéro de dossier repris tel quel du fichier, tronqué à la taille de la colonne (30). */
    private String numeroImporte(String numero) {
        return tronquer(numero.trim(), 30);
    }

    /**
     * Lieu par libellé dans la province (ville) de la feuille ; créé s'il manque.
     * Résultats mis en cache : la même feuille répète très souvent le même lieu.
     */
    private Lieu lieuParOuCreer(Ville ville, String libelle, Ctx ctx) {
        String cle = ville.getIdVille() + "\u0000" + libelle.toLowerCase();
        return ctx.lieuxCache.computeIfAbsent(cle, k ->
                lieuRepository.findByVilleIdVilleAndLibelleIgnoreCase(ville.getIdVille(), libelle)
                        .orElseGet(() -> {
                            // Feuille « Autre » : le lieu identifie lui-même la province
                            // (Farafangana → Fianarantsoa, Vatomandry → Toamasina, …)
                            if ("autre".equalsIgnoreCase(ville.getNomVille())) {
                                List<Lieu> globaux = lieuRepository.findByLibelleIgnoreCase(libelle);
                                if (!globaux.isEmpty()) return globaux.get(0);
                            }
                            Lieu cree = lieuRepository.save(Lieu.builder()
                                    .libelle(libelle)
                                    .ville(ville)
                                    .build());
                            ctx.lieuxImportes++;
                            return cree;
                        }));
    }

    /**
     * Libellé de lieu normalisé au format du référentiel : chaque ligne de la
     * cellule est trimmée puis les lignes sont jointes par « / »
     * (ex. « Ankasina\nAnjezika » → « Ankasina / Anjezika »).
     */
    private String normaliserLieu(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String p : s.split("\\r?\\n")) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" / ");
            sb.append(t);
        }
        return sb.toString();
    }

    private Ville trouverOuCreerVille(String nomVille) {
        return villeRepository.findByNomVilleIgnoreCase(nomVille)
                .orElseGet(() -> villeRepository.save(Ville.builder().nomVille(nomVille).build()));
    }

    private StatutDossier statutDefaut() {
        return statutDossierRepository.findAll().stream()
                .filter(s -> "En cours".equals(s.getCode()))
                .findFirst().orElse(null);
    }

    /** Vrai si la feuille correspond à l'onglet des prix au m². */
    private boolean estFeuillePrix(String nomFeuille) {
        return normaliser(nomFeuille).toLowerCase().contains("prix");
    }

    /** Nom normalisé : accents retirés, espaces/ponctuation → tiret. */
    private String normaliser(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.replaceAll("[\\s_°]+", "-").replaceAll("-+", "-");
    }

    /** ID Integer → UUID (convention propriete : 00000000-0000-0000-0000-{id:012d}). */
    private UUID integerToUuid(Integer id) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", id));
    }

    private Utilisateur utilisateurConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        return utilisateurRepository.findByNomUtilisateur(auth.getName()).orElse(null);
    }

    /** Valeur d'une colonne mappée (chaîne nettoyée : retours ligne → espace). */
    private String champ(Row row, Map<String, Integer> c, String cle) {
        if (!c.containsKey(cle)) return "";
        String s = texteCellule(row.getCell(c.get(cle)));
        return s.replaceAll("\\s*\\n\\s*", " ").trim();
    }

    /** Texte d'une cellule (nombres entiers sans décimale parasite). */
    private String texteCellule(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    private BigDecimal nombre(Row row, int idx) {
        if (row == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue().replace("\u00A0", " ")
                    .replaceAll("[\\s]", "").replace(",", ".").trim();
            if (s.isEmpty() || "-".equals(s)) return null;
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer entier(Row row, int idx) {
        BigDecimal n = nombre(row, idx);
        return n == null ? null : n.intValue();
    }

    /** Date Excel (série numérique, même non formatée) ou texte JJ/MM/AAAA. */
    private LocalDate date(Row row, int idx) {
        if (row == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                double v = cell.getNumericCellValue();
                if (DateUtil.isCellDateFormatted(cell) || (v >= 20000 && v <= 60000)) {
                    // Série Excel : jours écoulés depuis le 1899-12-30
                    return LocalDate.of(1899, 12, 30).plusDays((long) v);
                }
            }
        } catch (Exception ignored) {
            // pas une date
        }
        String s = texteCellule(cell);
        for (String pattern : new String[]{"d/M/yyyy", "d-M-yyyy", "d.M.yyyy"}) {
            try {
                return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // pattern suivant
            }
        }
        return null;
    }

    /** Tronque une chaîne à la taille maximale de la colonne cible. */
    private String tronquer(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Message le plus profond d'une chaîne d'exceptions (pour le résumé). */
    private String messageRacine(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
