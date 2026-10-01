-- ══════════════════════════════════════════════════════════════
-- V29 — Descente terrain : liaison dossier / demandeur optionnelle
-- ══════════════════════════════════════════════════════════════
-- Une descente relevée hors ligne peut être synchronisée sans dossier
-- (aucun numéro embarqué, ou numéro inconnu du serveur). Elle est alors
-- créée sans liaison et l'agent l'associe ensuite au bon dossier depuis
-- l'application web (bouton « Associer à un dossier »).
-- id_utilisateur_creation reste NOT NULL : la descente est toujours
-- imputable à l'agent connecté sur le mobile.

ALTER TABLE descente_terrain ALTER COLUMN id_personne DROP NOT NULL;
ALTER TABLE descente_terrain ALTER COLUMN id_dossier_parcelle DROP NOT NULL;
