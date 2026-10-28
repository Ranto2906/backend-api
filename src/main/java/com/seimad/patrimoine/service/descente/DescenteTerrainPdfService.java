package com.seimad.patrimoine.service.descente;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Chunk;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.seimad.patrimoine.dto.descente.DescenteTerrainDTO;
import com.seimad.patrimoine.dto.dossier.DossierDTO;
import com.seimad.patrimoine.entity.notification.Photo;
import com.seimad.patrimoine.repository.notification.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Fiche PDF officielle d'une DESCENTE TERRAIN — même papier à en-tête que la
 * fiche de demande, mais un contenu propre au terrain :
 *
 * <ol>
 *   <li>Identification de la demande — sans date de validation, montant total,
 *       avancement, statut ni dossier fourni (informations non utiles sur le
 *       terrain) ;</li>
 *   <li>Demandeurs ;</li>
 *   <li>Biens demandés ;</li>
 *   <li>Constat de la descente (observation de l'agent) ;</li>
 *   <li>Photos prises pendant la descente.</li>
 * </ol>
 *
 * Bloc de signatures : seul l'agent terrain signe (pas de signature
 * demandeur, pas de visa Directeur Général).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DescenteTerrainPdfService {

    private final PhotoRepository photoRepository;

    @Value("${app.stockage-photos:uploads/photos}")
    private String stockagePhotos;

    /** Type d'entité photo utilisé par le module descentes terrain. */
    private static final String ENTITE_DESCENTE = "descente_terrain";

    // ── Charte sobre « format national » : noir / blanc / gris ──
    private static final Color NOIR = new Color(17, 17, 17);
    private static final Color GRIS_TEXTE = new Color(90, 90, 90);
    private static final Color GRIS_FILET = new Color(140, 140, 140);
    private static final Color GRIS_EN_TETE = new Color(234, 234, 234);
    private static final Color BLEU_SEIMAD = new Color(31, 54, 122);
    private static final Color BLANC = Color.WHITE;

    private static final float PADDING_CELLULE = 4f;
    private static final float LARGEUR_FILET = 0.6f;

    /** Dimensions max d'une photo insérée dans la fiche (à l'échelle 1). */
    private static final float PHOTO_MAX_L = 215f;
    private static final float PHOTO_MAX_H = 150f;

    /**
     * Échelles de compacité successives (polices, espaces) : la fiche doit
     * tenir sur UNE page A4, on régénère donc avec une mise en page réduite
     * jusqu'à y parvenir. Le texte descend jusque 70 % pour rester lisible.
     */
    private static final float[] ECHELLES = {1f, 0.92f, 0.85f, 0.79f, 0.74f, 0.70f};

    /**
     * Échelle des photos pour chaque tentative : les images consomment
     * l'essentiel de la hauteur quand il y en a plusieurs, on les réduit donc
     * bien plus fortement que le texte (miniatures) plutôt que d'écraser le texte.
     */
    private static final float[] ECHELLES_PHOTOS = {1f, 0.75f, 0.55f, 0.42f, 0.32f, 0.25f};

    /** Échelle appliquée à la génération courante (1 = pleine taille). */
    private float zoom = 1f;

    /** Échelle des photos pour la génération courante (1 = pleine taille). */
    private float zoomPhoto = 1f;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private byte[] logoCache;

    /**
     * Génère la fiche PDF de la descente sur UNE SEULE page A4. Si le contenu
     * déborde, la mise en page (polices, espaces puis photos en priorité) est
     * réduite puis le document est régénéré jusqu'à tenir en une page.
     *
     * @param generePar nom de l'utilisateur connecté (pied de page)
     */
    public byte[] genererPdf(DescenteTerrainDTO descente, String generePar) {
        byte[] dernier = null;
        for (int i = 0; i < ECHELLES.length; i++) {
            dernier = construire(descente, generePar, ECHELLES[i], ECHELLES_PHOTOS[i]);
            if (compterPages(dernier) <= 1) {
                if (i > 0) {
                    log.info("Fiche descente {} compilée en une page A4 (échelle texte {} / photos {})",
                            descente.getReference(), ECHELLES[i], ECHELLES_PHOTOS[i]);
                }
                return dernier;
            }
        }
        log.warn("Fiche descente {} : contenu trop long pour une seule page même à l'échelle texte {} / photos {} — document multi-pages conservé",
                descente.getReference(), ECHELLES[ECHELLES.length - 1], ECHELLES_PHOTOS[ECHELLES_PHOTOS.length - 1]);
        return dernier;
    }

    /** Une tentative de génération, sur une instance isolée (thread-safe). */
    private byte[] construire(DescenteTerrainDTO descente, String generePar, float echelle, float echellePhoto) {
        DescenteTerrainPdfService generateur = new DescenteTerrainPdfService(photoRepository);
        generateur.stockagePhotos = this.stockagePhotos;
        generateur.logoCache = this.logoCache;
        generateur.zoom = echelle;
        generateur.zoomPhoto = echellePhoto;
        return generateur.genererDocument(descente, generePar);
    }

    /** Nombre de pages d'un PDF (MAX_VALUE si illisible → on continue l'essai). */
    private static int compterPages(byte[] pdf) {
        try {
            PdfReader reader = new PdfReader(pdf);
            int pages = reader.getNumberOfPages();
            reader.close();
            return pages;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    /** Écrit le document à l'échelle courante ({@link #zoom}). */
    private byte[] genererDocument(DescenteTerrainDTO descente, String generePar) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 34, 40);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PiedPage(texte(descente.getReference()), texte(generePar)));
            document.open();

            enTete(document, descente);
            titreSection(document, "1. Identification de la demande");
            sectionIdentification(document, descente);
            titreSection(document, "2. Demandeurs");
            sectionDemandeurs(document, descente);
            titreSection(document, "3. Biens demandés");
            sectionBiens(document, descente);
            titreSection(document, "4. Constat de la descente");
            sectionConstat(document, descente);
            titreSection(document, "5. Photos de la descente");
            sectionPhotos(document, descente);
            blocSignatures(document, descente);

            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Échec de génération du PDF de la descente id={}", descente.getIdDescente(), e);
            throw new RuntimeException("Impossible de générer le document PDF", e);
        }
    }

    // ══════════════════════════════════════════════════════════
    // En-tête institutionnel
    // ══════════════════════════════════════════════════════════

    private void enTete(Document document, DescenteTerrainDTO d) throws DocumentException, IOException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{20, 80});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        Image logo = chargerLogo();
        if (logo != null) {
            logoCell.addElement(logo);
        }
        t.addCell(logoCell);

        PdfPCell droite = celluleSansBord(new Phrase[]{
                new Phrase("SOCIETE D'EQUIPEMENT IMMOBILIER DE MADAGASCAR",
                        police(14, Font.BOLD, BLEU_SEIMAD)),
        });
        droite.setHorizontalAlignment(Element.ALIGN_CENTER);
        droite.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(droite);

        document.add(t);
        document.add(espace(6f));

        PdfPTable ref = new PdfPTable(2);
        ref.setWidthPercentage(100);
        ref.setWidths(new float[]{62, 38});
        PdfPCell gauche = celluleSansBord(new Phrase[]{
                new Phrase("FICHE DE DESCENTE TERRAIN N° " + valeur(d.getReference()),
                        police(11, Font.BOLD, NOIR)),
                new Phrase("Service de la demande foncière", police(8, Font.NORMAL, GRIS_TEXTE)),
        });
        gauche.setVerticalAlignment(Element.ALIGN_MIDDLE);
        PdfPCell dateCell = celluleSansBord(new Phrase[]{
                new Phrase("Établie le " + LocalDate.now().format(DATE), police(9, Font.NORMAL, GRIS_TEXTE)),
        });
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        ref.addCell(gauche);
        ref.addCell(dateCell);
        ref.setSpacingAfter(4f * zoom);
        document.add(ref);
    }

    private Image chargerLogo() {
        try {
            if (logoCache == null) {
                try (InputStream in = new ClassPathResource("logo/logoseimad.jpg").getInputStream()) {
                    logoCache = in.readAllBytes();
                }
            }
            Image img = Image.getInstance(logoCache);
            img.scaleToFit(72f * zoom, 67f * zoom);
            img.setAlignment(Element.ALIGN_CENTER);
            return img;
        } catch (Exception e) {
            log.warn("Logo SEIMAD introuvable dans les ressources, en-tête généré sans logo : {}", e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════
    // Sections
    // ══════════════════════════════════════════════════════════

    /**
     * 1. Identification — volontairement allégée : ni date de validation, ni
     * montant total, ni avancement, ni statut, ni dossier fourni.
     */
    private void sectionIdentification(Document document, DescenteTerrainDTO d) throws DocumentException {
        DossierDTO dossier = d.getDossier();
        if (dossier == null) {
            document.add(paragraphe("Aucun dossier rattaché à cette descente.", italique()));
            return;
        }
        PdfPTable t = tableau(new float[]{21, 29, 21, 29});
        ajouterPaire(t, "N° dossier", valeur(dossier.getNumeroDossier()), false);
        ajouterPaire(t, "Type de dossier", valeur(dossier.getLibelleTypeDossier()), false);
        ajouterPaire(t, "Ville", valeur(d.getNomVille() != null ? d.getNomVille() : dossier.getNomVille()), false);
        ajouterPaire(t, "Date de demande",
                dossier.getDateDemande() != null ? dossier.getDateDemande().format(DATE) : "—", false);
        ajouterPaire(t, "Transmission", valeur(dossier.getTransmission()), false);
        ajouterPaire(t, "Résultat de l'étude", valeur(dossier.getResultatEtude()), false);
        document.add(t);
    }

    private void sectionDemandeurs(Document document, DescenteTerrainDTO d) throws DocumentException {
        List<DossierDTO.DemandeurDTO> demandeurs = d.getDossier() != null ? d.getDossier().getDemandeurs() : null;
        if (demandeurs == null || demandeurs.isEmpty()) {
            document.add(paragraphe("Aucun demandeur enregistré.", italique()));
            return;
        }
        PdfPTable t = tableau(new float[]{24, 12, 13, 16, 15, 20});
        String[] entetes = {"Nom", "Rôle", "Type", "Contact", "Email", "Adresse"};
        for (String e : entetes) {
            t.addCell(celluleEntete(e));
        }
        for (DossierDTO.DemandeurDTO p : demandeurs) {
            t.addCell(cellule(police(8.5f, Font.BOLD, NOIR), valeur(p.getNom())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(p.getRole())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(p.getLibelleTypePersonne())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(p.getContact())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(p.getEmail())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(p.getAdresse())));
        }
        document.add(t);
    }

    private void sectionBiens(Document document, DescenteTerrainDTO d) throws DocumentException {
        List<DossierDTO.ParcelleDemandeeDTO> parcelles =
                d.getDossier() != null ? d.getDossier().getParcelles() : null;
        if (parcelles == null || parcelles.isEmpty()) {
            document.add(paragraphe("Aucune parcelle rattachée à ce dossier.", italique()));
            return;
        }
        PdfPTable t = tableau(new float[]{34, 24, 16, 26});
        String[] entetes = {"Propriété", "N° titre / propriété", "Lot", "Superficie (m²)"};
        for (String e : entetes) {
            t.addCell(celluleEntete(e));
        }
        BigDecimal superficieTotale = BigDecimal.ZERO;
        boolean superficieComplete = true;
        for (DossierDTO.ParcelleDemandeeDTO p : parcelles) {
            t.addCell(cellule(police(8.5f, Font.BOLD, NOIR), valeur(p.getNomPropriete())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(p.getNumeroPropriete())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(p.getNumeroLot())));
            if (p.getSuperficieM2() != null) {
                superficieTotale = superficieTotale.add(p.getSuperficieM2());
                t.addCell(celluleDroite(police(8.5f, Font.NORMAL, NOIR), nombre(p.getSuperficieM2())));
            } else {
                superficieComplete = false;
                t.addCell(celluleDroite(police(8.5f, Font.NORMAL, GRIS_TEXTE), "—"));
            }
        }
        PdfPCell lbl = cellule(police(8.5f, Font.BOLD, NOIR), "Superficie totale", GRIS_EN_TETE);
        lbl.setColspan(3);
        t.addCell(lbl);
        t.addCell(celluleDroite(police(8.5f, Font.BOLD, NOIR),
                superficieComplete ? nombre(superficieTotale) + " m²" : nombre(superficieTotale) + " m² (+)",
                GRIS_EN_TETE));
        document.add(t);
    }

    /** 4. Constat de la descente : références du constat + observation agent. */
    private void sectionConstat(Document document, DescenteTerrainDTO d) throws DocumentException {
        PdfPTable t = tableau(new float[]{21, 29, 21, 29});
        ajouterPaire(t, "Référence", valeur(d.getReference()), false);
        ajouterPaire(t, "Date de la descente",
                d.getDateDescente() != null ? d.getDateDescente().format(DATE) : "—", false);
        ajouterPaire(t, "Statut du constat", valeur(d.getStatutConstat()), false);
        ajouterPaire(t, "Mode de saisie",
                "offline".equalsIgnoreCase(d.getMode()) ? "Hors ligne" : "En ligne", false);
        ajouterPaire(t, "Agent", valeur(d.getNomUtilisateurCreation()), false);
        ajouterPaire(t, "Personne accompagnée", valeur(d.getNomPersonne()), false);
        document.add(t);
        document.add(espace(4f));

        Paragraph obsTitre = new Paragraph("Observation de la descente", police(8.5f, Font.BOLD, NOIR));
        obsTitre.setSpacingBefore(2f);
        obsTitre.setSpacingAfter(2f);
        document.add(obsTitre);
        String observation = d.getObservation() != null && !d.getObservation().isBlank()
                ? d.getObservation() : "Aucune observation consignée.";
        document.add(paragraphe(observation, police(8.5f, Font.NORMAL, NOIR)));
    }

    /** 5. Photos prises pendant la descente (grille 2 colonnes + légendes). */
    private void sectionPhotos(Document document, DescenteTerrainDTO d) throws DocumentException {
        List<Photo> photos = photoRepository
                .findByEntiteTypeAndEntiteIdOrderByDateCreationAsc(ENTITE_DESCENTE, d.getIdDescente());
        if (photos.isEmpty()) {
            document.add(paragraphe("Aucune photo enregistrée pour cette descente.", italique()));
            return;
        }
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingBefore(2f * zoom);
        t.setSplitRows(true);

        int numero = 1;
        for (Photo photo : photos) {
            PdfPCell cell = new PdfPCell();
            cell.setBorderColor(GRIS_FILET);
            cell.setBorderWidth(LARGEUR_FILET);
            cell.setPadding(PADDING_CELLULE * zoom);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_TOP);

            Image img = chargerPhoto(photo);
            if (img != null) {
                cell.addElement(img);
            } else {
                cell.addElement(new Phrase("Photo indisponible (fichier manquant ou illisible)",
                        police(8, Font.ITALIC, GRIS_TEXTE)));
            }

            StringBuilder legende = new StringBuilder("Photo " + numero);
            if (photo.getTypePhoto() != null && !photo.getTypePhoto().isBlank()) {
                legende.append(" — ").append(photo.getTypePhoto());
            }
            if (photo.getDatePrise() != null) {
                legende.append(" (").append(photo.getDatePrise().format(DATE)).append(')');
            }
            cell.addElement(new Phrase(texte(legende.toString()), police(7.5f, Font.BOLD, NOIR)));
            if (photo.getObservation() != null && !photo.getObservation().isBlank()) {
                cell.addElement(new Phrase(texte(photo.getObservation()), police(7.5f, Font.NORMAL, GRIS_TEXTE)));
            }
            t.addCell(cell);
            numero++;
        }
        // Cellule vide si nombre impair : ferme proprement la dernière ligne.
        if (photos.size() % 2 == 1) {
            PdfPCell vide = new PdfPCell();
            vide.setBorder(Rectangle.NO_BORDER);
            t.addCell(vide);
        }
        document.add(t);
    }

    /** Charge le fichier d'une photo (garde anti traversal de chemin). */
    private Image chargerPhoto(Photo photo) {
        if (photo.getCheminFichier() == null || photo.getCheminFichier().isBlank()) {
            return null;
        }
        try {
            Path racine = Paths.get(stockagePhotos).toAbsolutePath();
            Path fichier = racine.resolve(photo.getCheminFichier()).normalize();
            if (!fichier.startsWith(racine) || !Files.exists(fichier)) {
                log.warn("Photo {} introuvable : {}", photo.getIdPhoto(), photo.getCheminFichier());
                return null;
            }
            Image img = Image.getInstance(Files.readAllBytes(fichier));
            img.scaleToFit(PHOTO_MAX_L * zoomPhoto, PHOTO_MAX_H * zoomPhoto);
            img.setAlignment(Element.ALIGN_CENTER);
            return img;
        } catch (Exception e) {
            log.warn("Lecture de la photo {} impossible : {}", photo.getIdPhoto(), e.getMessage());
            return null;
        }
    }

    /** Bloc de signatures : l'agent terrain seul (ni demandeur, ni Directeur Général). */
    private void blocSignatures(Document document, DescenteTerrainDTO d) throws DocumentException {
        document.add(espace(16f));
        String ville = d.getNomVille() != null && !d.getNomVille().isBlank() ? d.getNomVille() : "Antananarivo";
        Paragraph fait = new Paragraph("Fait à " + texte(ville) + ", le " + LocalDate.now().format(DATE),
                police(9, Font.NORMAL, NOIR));
        fait.setAlignment(Element.ALIGN_RIGHT);
        fait.setSpacingAfter(12f * zoom);
        document.add(fait);

        // Aligné à droite : un Paragraph posé directement dans le document respecte
        // son alignement (une cellule contenant plusieurs éléments crée une table
        // imbriquée dont l'alignement de droite est ignoré — d'où l'ancien bloc à gauche).
        document.add(ligneSignature("L'Agent SEIMAD", police(9, Font.BOLD, NOIR), 0f));
        document.add(ligneSignature("Signature et cachet", police(8, Font.ITALIC, GRIS_TEXTE), 0f));
        // Zone réservée à la signature manuscrite et au cachet (espace() ne produit
        // aucune hauteur sur une ligne vide → marge haute explicite).
        document.add(ligneSignature(texte(d.getNomUtilisateurCreation()), police(8.5f, Font.NORMAL, NOIR), 26f));
        document.add(ligneSignature("________________________", police(9, Font.NORMAL, GRIS_TEXTE), 0f));
    }

    /**
     * Ligne du bloc de signature, alignée sur le bord droit de la page.
     *
     * @param margeHauteur marge au-dessus de la ligne (en points, avant échelle),
     *                     pour réserver la zone de signature
     */
    private Paragraph ligneSignature(String libelle, Font font, float margeHauteur) {
        Paragraph p = new Paragraph(libelle == null ? "" : libelle, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingBefore(margeHauteur * zoom);
        p.setSpacingAfter(0);
        return p;
    }

    // ══════════════════════════════════════════════════════════
    // Brique typographique (même charte que la fiche de demande)
    // ══════════════════════════════════════════════════════════

    private void titreSection(Document document, String texte) throws DocumentException {
        Paragraph p = new Paragraph(new Chunk(texte, police(10, Font.BOLD, NOIR)));
        p.setSpacingBefore(11f * zoom);
        p.setSpacingAfter(3f * zoom);
        document.add(p);
        document.add(espace(2f));
    }

    private PdfPTable tableau(float[] largeurs) throws DocumentException {
        PdfPTable t = new PdfPTable(largeurs.length);
        t.setWidthPercentage(100);
        t.setWidths(largeurs);
        t.setSpacingBefore(2f * zoom);
        t.setSplitRows(true);
        return t;
    }

    private PdfPCell celluleEntete(String texte) {
        PdfPCell c = cellule(police(8.5f, Font.BOLD, NOIR), texte, GRIS_EN_TETE);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private void ajouterPaire(PdfPTable t, String label, String valeur, boolean large) {
        t.addCell(cellule(police(8.5f, Font.BOLD, NOIR), label, GRIS_EN_TETE));
        PdfPCell val = cellule(police(8.5f, Font.NORMAL, NOIR), valeur);
        if (large) {
            val.setColspan(3);
        }
        t.addCell(val);
    }

    private static PdfPCell celluleSansBord(Phrase[] phrases) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1f);
        for (Phrase p : phrases) {
            c.addElement(p);
        }
        return c;
    }

    private PdfPCell cellule(Font font, String texte) {
        return cellule(font, texte, null);
    }

    private PdfPCell cellule(Font font, String texte, Color fond) {
        PdfPCell c = new PdfPCell(new Phrase(texte == null ? "" : texte, font));
        c.setBorderColor(GRIS_FILET);
        c.setBorderWidth(LARGEUR_FILET);
        c.setPadding(PADDING_CELLULE * zoom);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (fond != null) {
            c.setBackgroundColor(fond);
        }
        return c;
    }

    private PdfPCell celluleDroite(Font font, String texte) {
        return celluleDroite(font, texte, null);
    }

    private PdfPCell celluleDroite(Font font, String texte, Color fond) {
        PdfPCell c = cellule(font, texte, fond);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private Paragraph paragraphe(String texte, Font font) {
        Paragraph p = new Paragraph(texte == null ? "" : texte, font);
        p.setSpacingBefore(2f * zoom);
        p.setSpacingAfter(2f * zoom);
        return p;
    }

    private Paragraph espace(float hauteur) {
        Paragraph p = new Paragraph("", police(1, Font.NORMAL, BLANC));
        p.setLeading(hauteur * zoom);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        return p;
    }

    private Font police(float taille, int style, Color couleur) {
        return FontFactory.getFont(FontFactory.HELVETICA, taille * zoom, style, couleur);
    }

    private Font italique() {
        return police(8.5f, Font.ITALIC, GRIS_TEXTE);
    }

    // ══════════════════════════════════════════════════════════
    // Formatage / normalisation
    // ══════════════════════════════════════════════════════════

    private static String valeur(String s) {
        return s == null || s.isBlank() ? "—" : texte(s);
    }

    private static String nombre(BigDecimal v) {
        return String.format(Locale.FRANCE, "%,.2f", v)
                .replace('\u202f', ' ').replace('\u00a0', ' ');
    }

    /** Normalise un texte vers Latin-1 (accents français corrects sans police embarquée). */
    private static String texte(String s) {
        if (s == null) {
            return "";
        }
        String nettoye = s
                .replace('\u2019', '\'').replace('\u2018', '\'')
                .replace('\u201c', '"').replace('\u201d', '"')
                .replace('\u2013', '-').replace('\u2014', '-')
                .replace('\u202f', ' ').replace('\u00a0', ' ');
        StringBuilder sb = new StringBuilder(nettoye.length());
        for (char c : nettoye.toCharArray()) {
            sb.append(c >= 32 && c <= 255 ? c : '?');
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════
    // Pied de page paginé
    // ══════════════════════════════════════════════════════════

    /** Police fixe du pied de page — indépendante de l'échelle de compacité. */
    private static Font policePied(float taille, int style, Color couleur) {
        return FontFactory.getFont(FontFactory.HELVETICA, taille, style, couleur);
    }

    /** Cellule sans bordure du pied de page — indépendante de l'échelle. */
    private static PdfPCell cellulePied(Font font, String texte) {
        PdfPCell c = new PdfPCell(new Phrase(texte == null ? "" : texte, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    private static class PiedPage extends PdfPageEventHelper {
        private final String reference;
        private final String generePar;

        private PiedPage(String reference, String generePar) {
            this.reference = reference;
            this.generePar = generePar;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable pied = new PdfPTable(3);
            pied.setWidthPercentage(100);
            pied.setWidths(new float[]{38, 34, 28});
            PdfPCell g = cellulePied(policePied(7, Font.NORMAL, GRIS_TEXTE),
                    "SEIMAD — Fiche de descente n° " + reference);
            PdfPCell c = cellulePied(policePied(7, Font.NORMAL, GRIS_TEXTE),
                    "Document généré le " + LocalDateTime.now().format(DATE_HEURE) + " par " + generePar);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell d = cellulePied(policePied(7, Font.NORMAL, GRIS_TEXTE),
                    "Page " + writer.getPageNumber());
            d.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pied.addCell(g);
            pied.addCell(c);
            pied.addCell(d);
            pied.setTotalWidth(document.right() - document.left());
            pied.writeSelectedRows(0, -1, document.left(), document.bottom() - 16, writer.getDirectContent());
        }
    }
}
