package com.seimad.patrimoine.service.dossier;

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
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.seimad.patrimoine.dto.dossier.DossierDTO;
import com.seimad.patrimoine.repository.referentiel.PrixM2Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Document officiel PDF d'une demande (fiche A4 « format national ») :
 * en-tête « Société d'Équipement Immobilier de Madagascar » avec logo, identification du
 * dossier, demandeurs, biens demandés, évaluation, suivi du workflow,
 * observations et bloc de signatures.
 *
 * <p>Charte sobre noir / blanc / gris administrative : pas de fonds colorés,
 * filets fins, textes centrés en capitales pour l'en-tête institutionnel.
 * Tous les tableaux sont dimensionnés en pourcentage de la page pour tenir
 * strictement dans le format A4.</p>
 *
 * <p>Polices standard Helvetica : le texte est normalisé vers Latin-1
 * (guillemets typographiaux, tirets cadratins, espaces insécables…) afin que
 * les accents français soient correctement encodés sans police embarquée.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DossierPdfService {

    private final PrixM2Repository prixM2Repository;

    // ── Charte sobre « format national » : noir / blanc / gris ──
    private static final Color NOIR = new Color(17, 17, 17);          // texte principal
    private static final Color GRIS_TEXTE = new Color(90, 90, 90);    // mentions secondaires
    private static final Color GRIS_FILET = new Color(140, 140, 140); // filets de tableau
    private static final Color GRIS_EN_TETE = new Color(234, 234, 234); // fond d'en-tête de tableau
    private static final Color BLEU_SEIMAD = new Color(31, 54, 122);  // bleu marine du papier à en-tête
    private static final Color BLANC = Color.WHITE;

    private static final float PADDING_CELLULE = 4f;
    private static final float LARGEUR_FILET = 0.6f;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private byte[] logoCache;

    /**
     * Génère la fiche PDF du dossier. {@code generePar} est le nom de
     * l'utilisateur connecté (mentionné dans le pied de page).
     */
    public byte[] genererPdf(DossierDTO dossier, String generePar) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Marges A4 : 1,6 cm latérales, 1,4 cm haut/bas — tout le contenu
            // reste strictement dans le papier.
            Document document = new Document(PageSize.A4, 45, 45, 40, 48);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PiedPage(texte(dossier.getNumeroDossier()), texte(generePar)));
            document.open();

            enTete(document, dossier);
            titreSection(document, "1. Identification de la demande");
            sectionIdentification(document, dossier);
            titreSection(document, "2. Demandeurs");
            sectionDemandeurs(document, dossier);
            titreSection(document, "3. Biens demandés");
            sectionBiens(document, dossier);
            titreSection(document, "4. Évaluation");
            sectionEvaluation(document, dossier);
            titreSection(document, "5. Suivi de la demande");
            sectionSuivi(document, dossier);
            titreSection(document, "6. Observations");
            sectionObservations(document, dossier);
            blocSignatures(document, dossier);

            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Échec de génération du PDF du dossier id={}", dossier.getIdDossier(), e);
            throw new RuntimeException("Impossible de générer le document PDF", e);
        }
    }

    // ══════════════════════════════════════════════════════════
    // En-tête institutionnel
    // ══════════════════════════════════════════════════════════

    private void enTete(Document document, DossierDTO d) throws DocumentException, IOException {
        // Bandeau identique au papier à en-tête SEIMAD : logo à gauche,
        // raison sociale en bleu marine sur la même ligne, à droite.
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{20, 80});

        // Logo SEIMAD (gauche)
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        Image logo = chargerLogo();
        if (logo != null) {
            logoCell.addElement(logo);
        }
        t.addCell(logoCell);

        // Raison sociale en bleu marine (couleur du papier à en-tête)
        PdfPCell droite = celluleSansBord(new Phrase[]{
            new Phrase("SOCIETE D'EQUIPEMENT IMMOBILIER DE MADAGASCAR",
                    police(14, Font.BOLD, BLEU_SEIMAD)),
        });
        droite.setHorizontalAlignment(Element.ALIGN_CENTER);
        droite.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(droite);

        document.add(t);
        document.add(espace(8f));

        // Identification du document : premier élément du contenu, sous le bandeau
        PdfPTable ref = new PdfPTable(2);
        ref.setWidthPercentage(100);
        ref.setWidths(new float[]{62, 38});
        PdfPCell gauche = celluleSansBord(new Phrase[]{
            new Phrase("FICHE DE DEMANDE N° " + texte(d.getNumeroDossier()),
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
        ref.setSpacingAfter(6f);
        document.add(ref);
    }


    /** Logo SEIMAD chargé depuis les ressources (largement réduit, 17 mm de haut). */
    private Image chargerLogo() {
        try {
            if (logoCache == null) {
                try (InputStream in = new ClassPathResource("logo/logoseimad.jpg").getInputStream()) {
                    logoCache = in.readAllBytes();
                }
            }
            Image img = Image.getInstance(logoCache);
            img.scaleToFit(72f, 67f);
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

    private void sectionIdentification(Document document, DossierDTO d) throws DocumentException {
        PdfPTable t = tableau(new float[]{21, 29, 21, 29});
        ajouterPaire(t, "Type de dossier", valeur(d.getLibelleTypeDossier()), false);
        ajouterPaire(t, "Ville", valeur(d.getNomVille()), false);
        ajouterPaire(t, "Date de demande",
                d.getDateDemande() != null ? d.getDateDemande().format(DATE) : "—", false);
        ajouterPaire(t, "Statut", valeurStatut(d.getLibelleStatut()), false);
        ajouterPaire(t, "Transmission", valeur(d.getTransmission()), false);
        ajouterPaire(t, "Date de validation",
                d.getDateValidation() != null ? d.getDateValidation().format(DATE) : "—", false);
        ajouterPaire(t, "Montant total", d.getMontantTotal() != null ? nombre(d.getMontantTotal()) + " Ar" : "—", false);
        ajouterPaire(t, "Dossier fourni", valeur(d.getLibelleDossierFournie()), false);
        ajouterPaire(t, "Résultat de l'étude", valeur(d.getResultatEtude()), false);
        ajouterPaire(t, "Avancement",
                (d.getNombreEtapesRealisees() != null ? d.getNombreEtapesRealisees() : 0) + " étape(s) réalisée(s)"
                        + (d.getEtapeActuelleLibelle() != null && !d.getEtapeActuelleLibelle().isBlank()
                            ? " - en cours : " + d.getEtapeActuelleLibelle() : ""), true);
        document.add(t);
    }

    private void sectionDemandeurs(Document document, DossierDTO d) throws DocumentException {
        List<DossierDTO.DemandeurDTO> demandeurs = d.getDemandeurs();
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

    private void sectionBiens(Document document, DossierDTO d) throws DocumentException {
        List<DossierDTO.ParcelleDemandeeDTO> parcelles = d.getParcelles();
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
        // Ligne de total
        PdfPCell lbl = cellule(police(8.5f, Font.BOLD, NOIR), "Superficie totale", GRIS_EN_TETE);
        lbl.setColspan(3);
        t.addCell(lbl);
        t.addCell(celluleDroite(police(8.5f, Font.BOLD, NOIR),
                superficieComplete ? nombre(superficieTotale) + " m²" : nombre(superficieTotale) + " m² (+)", GRIS_EN_TETE));
        document.add(t);
    }

    private void sectionEvaluation(Document document, DossierDTO d) throws DocumentException {
        PdfPTable t = tableau(new float[]{34, 20, 46});
        // ── Prix au m² FIGÉ sur le dossier (en vigueur à son enregistrement).
        // Repli sur le prix courant uniquement pour les dossiers hérités sans snapshot. ──
        BigDecimal prixTtc = d.getPrixM2Ttc();
        BigDecimal prixHt = d.getPrixM2Ht();
        if (prixTtc == null && d.getIdVille() != null) {
            List<com.seimad.patrimoine.entity.referentiel.PrixM2> prix =
                    prixM2Repository.findByVilleIdVilleOrderByDateMajDesc(d.getIdVille());
            if (!prix.isEmpty()) {
                prixTtc = prix.get(0).getPrixTtc();
                prixHt = prix.get(0).getPrixHt();
            }
        }
        ajouterLigneInfo(t, "Prix au m² retenu (TTC)", prixTtc != null ? nombre(prixTtc) + " Ar/m²" : "—");
        ajouterLigneInfo(t, "Prix au m² retenu (HT)", prixHt != null ? nombre(prixHt) + " Ar/m²" : "—");
        ajouterLigneInfo(t, "Montant total de la demande",
                d.getMontantTotal() != null ? nombre(d.getMontantTotal()) + " Ar" : "—");
        document.add(t);
    }

    private void sectionSuivi(Document document, DossierDTO d) throws DocumentException {
        List<DossierDTO.SuiviDossierDTO> suivi = d.getSuivi();
        if (suivi == null || suivi.isEmpty()) {
            document.add(paragraphe("Aucune étape réalisée à ce jour.", italique()));
            return;
        }
        PdfPTable t = tableau(new float[]{24, 14, 15, 31, 16});
        String[] entetes = {"Étape", "Date", "Statut", "Commentaire", "Opérateur"};
        for (String e : entetes) {
            t.addCell(celluleEntete(e));
        }
        for (DossierDTO.SuiviDossierDTO s : suivi) {
            String libelle = (s.getOrdreEtape() != null ? s.getOrdreEtape() + ". " : "") + texte(s.getLibelleEtape());
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), libelle));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR),
                    s.getDateRealisation() != null ? s.getDateRealisation().format(DATE) : "—"));
            t.addCell(cellule(police(8.5f, Font.BOLD, NOIR), valeurStatut(s.getLibelleStatutCouleur())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(s.getCommentaire())));
            t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), valeur(s.getNomUtilisateur())));
        }
        document.add(t);
    }

    private void sectionObservations(Document document, DossierDTO d) throws DocumentException {
        String obs = d.getObservation() != null && !d.getObservation().isBlank()
                ? d.getObservation() : "Aucune observation.";
        document.add(paragraphe(obs, police(8.5f, Font.NORMAL, NOIR)));
    }

    private void blocSignatures(Document document, DossierDTO d) throws DocumentException {
        document.add(espace(22f));
        String ville = d.getNomVille() != null && !d.getNomVille().isBlank() ? d.getNomVille() : "Antananarivo";
        Paragraph fait = new Paragraph("Fait à " + texte(ville) + ", le " + LocalDate.now().format(DATE),
                police(9, Font.NORMAL, NOIR));
        fait.setAlignment(Element.ALIGN_RIGHT);
        fait.setSpacingAfter(16f);
        document.add(fait);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingBefore(8f);
        PdfPCell gauche = celluleSansBord(new Phrase[]{
            new Phrase("Le demandeur", police(9, Font.BOLD, NOIR)),
            new Phrase("Signature et cachet", police(8, Font.ITALIC, GRIS_TEXTE)),
            new Phrase("", police(9, Font.NORMAL, NOIR)),
            new Phrase("", police(9, Font.NORMAL, NOIR)),
            new Phrase("________________________", police(9, Font.NORMAL, GRIS_TEXTE)),
        });
        PdfPCell droite = celluleSansBord(new Phrase[]{
            new Phrase("Le Directeur Général", police(9, Font.BOLD, NOIR)),
            new Phrase("Visa et cachet — SEIMAD", police(8, Font.ITALIC, GRIS_TEXTE)),
            new Phrase("", police(9, Font.NORMAL, NOIR)),
            new Phrase("", police(9, Font.NORMAL, NOIR)),
            new Phrase("________________________", police(9, Font.NORMAL, GRIS_TEXTE)),
        });
        droite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(gauche);
        t.addCell(droite);
        document.add(t);
    }

    // ══════════════════════════════════════════════════════════
    // Brique typographique
    // ══════════════════════════════════════════════════════════

    private static void titreSection(Document document, String texte) throws DocumentException {
        Paragraph p = new Paragraph(new Chunk(texte, police(10, Font.BOLD, NOIR)));
        p.setSpacingBefore(13f);
        p.setSpacingAfter(3f);
        document.add(p);
        document.add(espace(2f));
    }

    private static PdfPTable tableau(float[] largeurs) throws DocumentException {
        PdfPTable t = new PdfPTable(largeurs.length);
        t.setWidthPercentage(100);
        t.setWidths(largeurs);
        t.setSpacingBefore(2f);
        t.setSplitRows(true);
        return t;
    }

    /** Cellule d'en-tête de tableau : fond gris clair, texte gras noir, centré. */
    private static PdfPCell celluleEntete(String texte) {
        PdfPCell c = cellule(police(8.5f, Font.BOLD, NOIR), texte, GRIS_EN_TETE);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    /** Statut : simple texte capitalisé (plus de pastille colorée). */
    private static String valeurStatut(String libelle) {
        if (libelle == null || libelle.isBlank()) {
            return "—";
        }
        return libelle.toUpperCase(Locale.FRANCE);
    }

    /** Ligne clé/valeur d'une identification (colonne large éventuellement). */
    private void ajouterPaire(PdfPTable t, String label, String valeur, boolean large) {
        t.addCell(cellule(police(8.5f, Font.BOLD, NOIR), label, GRIS_EN_TETE));
        PdfPCell val = cellule(police(8.5f, Font.NORMAL, NOIR), valeur);
        if (large) {
            val.setColspan(3);
        }
        t.addCell(val);
    }

    /** Ligne information d'un tableau 3 colonnes (34/20/46 : libellé, valeur, note vide). */
    private void ajouterLigneInfo(PdfPTable t, String label, String valeur) {
        t.addCell(cellule(police(8.5f, Font.BOLD, NOIR), label, GRIS_EN_TETE));
        t.addCell(celluleDroite(police(8.5f, Font.BOLD, NOIR), valeur));
        t.addCell(cellule(police(8.5f, Font.NORMAL, NOIR), ""));
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

    private static PdfPCell cellule(Font font, String texte) {
        return cellule(font, texte, null);
    }

    private static PdfPCell cellule(Font font, String texte, Color fond) {
        PdfPCell c = new PdfPCell(new Phrase(texte == null ? "" : texte, font));
        c.setBorderColor(GRIS_FILET);
        c.setBorderWidth(LARGEUR_FILET);
        c.setPadding(PADDING_CELLULE);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (fond != null) {
            c.setBackgroundColor(fond);
        }
        return c;
    }

    private static PdfPCell celluleDroite(Font font, String texte) {
        return celluleDroite(font, texte, null);
    }

    private static PdfPCell celluleDroite(Font font, String texte, Color fond) {
        PdfPCell c = cellule(font, texte, fond);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private static Paragraph paragraphe(String texte, Font font) {
        Paragraph p = new Paragraph(texte == null ? "" : texte, font);
        p.setSpacingBefore(2f);
        p.setSpacingAfter(2f);
        return p;
    }

    private static Paragraph espace(float hauteur) {
        Paragraph p = new Paragraph("", police(1, Font.NORMAL, BLANC));
        p.setLeading(hauteur);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        return p;
    }

    private static Font police(float taille, int style, Color couleur) {
        return FontFactory.getFont(FontFactory.HELVETICA, taille, style, couleur);
    }

    private static Font italique() {
        return police(8.5f, Font.ITALIC, GRIS_TEXTE);
    }

    // ══════════════════════════════════════════════════════════
    // Formatage / normalisation
    // ══════════════════════════════════════════════════════════

    /** Valeur affichable (« — » si vide). */
    private static String valeur(String s) {
        return s == null || s.isBlank() ? "—" : texte(s);
    }

    /** Nombre au format français : 1 234,56 (séparateur espace, décimale virgule). */
    private static String nombre(BigDecimal v) {
        return String.format(Locale.FRANCE, "%,.2f", v)
                .replace('\u202f', ' ').replace('\u00a0', ' ');
    }

    /**
     * Normalise un texte vers Latin-1 pour les polices de base : apostrophes et
     * guillemets typographiques, tirets, espaces insécables ; tout caractère
     * hors couverture devient « ? » (jamais d'exception d'encodage).
     */
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

    private static class PiedPage extends PdfPageEventHelper {
        private final String numero;
        private final String generePar;

        private PiedPage(String numero, String generePar) {
            this.numero = numero;
            this.generePar = generePar;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable pied = new PdfPTable(3);
            pied.setWidthPercentage(100);
            pied.setWidths(new float[]{38, 34, 28});
            PdfPCell g = cellule(police(7, Font.NORMAL, GRIS_TEXTE),
                    "SEIMAD — Fiche de demande n° " + numero);
            g.setBorder(Rectangle.NO_BORDER);
            g.setPadding(0);
            PdfPCell c = cellule(police(7, Font.NORMAL, GRIS_TEXTE),
                    "Document généré le " + LocalDateTime.now().format(DATE_HEURE) + " par " + generePar);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(0);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell d = cellule(police(7, Font.NORMAL, GRIS_TEXTE),
                    "Page " + writer.getPageNumber());
            d.setBorder(Rectangle.NO_BORDER);
            d.setPadding(0);
            d.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pied.addCell(g);
            pied.addCell(c);
            pied.addCell(d);
            // writeSelectedRows exige une largeur absolue (setTotalWidth) :
            // la largeur en pourcentage seule reste nulle ici.
            pied.setTotalWidth(document.right() - document.left());
            pied.writeSelectedRows(0, -1, document.left(), document.bottom() - 16, writer.getDirectContent());
        }
    }
}
