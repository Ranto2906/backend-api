-- ============================================================================
-- MIGRATION V9 : Nettoyage des doublons d'import Excel
--
-- L'import du fichier « Suivi des notifications Patrimoine foncier » n'était pas
-- idempotent : le ré-import du même fichier créait une seconde copie complète
-- (2 x notifications, 2 x suivis, 2 x avertissements).
--
-- On supprime ici les doublons en conservant la ligne la plus ancienne
-- (created_at) de chaque groupe identique :
--   notification : même titre foncier + parcelle + date de notification +
--                  même contenu + mêmes personnes liées
--   avertissement : même titre foncier + parcelle + date + même contenu +
--                   même personne avertie
-- Les enfants (suivi_notification, notification_personne, photo) des lignes
-- supprimées sont retirés en premier.
-- ============================================================================

-- ── 1. Suivis des notifications dupliquées ──
WITH doublons AS (
    SELECT id_notification FROM (
        SELECT n.id_notification,
               row_number() OVER (
                   PARTITION BY n.id_titre_foncier,
                                n.id_parcelle,
                                n.date_notification,
                                COALESCE(n.date_convocation::text, ''),
                                COALESCE(n.informations_occupants, ''),
                                COALESCE(n.constats, ''),
                                COALESCE(n.doleances, ''),
                                COALESCE(n.actions_entreprises, ''),
                                COALESCE(n.statut, ''),
                                COALESCE((SELECT string_agg(np.id_personne::text, ',' ORDER BY np.id_personne)
                                          FROM notification_personne np
                                          WHERE np.id_notification = n.id_notification), '')
                   ORDER BY n.created_at, n.id_notification) AS rn
        FROM notification_occupation n
    ) t
    WHERE t.rn > 1
)
DELETE FROM suivi_notification s
USING doublons d
WHERE s.id_notification = d.id_notification;

-- ── 2. Personnes liées des notifications dupliquées ──
WITH doublons AS (
    SELECT id_notification FROM (
        SELECT n.id_notification,
               row_number() OVER (
                   PARTITION BY n.id_titre_foncier,
                                n.id_parcelle,
                                n.date_notification,
                                COALESCE(n.date_convocation::text, ''),
                                COALESCE(n.informations_occupants, ''),
                                COALESCE(n.constats, ''),
                                COALESCE(n.doleances, ''),
                                COALESCE(n.actions_entreprises, ''),
                                COALESCE(n.statut, ''),
                                COALESCE((SELECT string_agg(np.id_personne::text, ',' ORDER BY np.id_personne)
                                          FROM notification_personne np
                                          WHERE np.id_notification = n.id_notification), '')
                   ORDER BY n.created_at, n.id_notification) AS rn
        FROM notification_occupation n
    ) t
    WHERE t.rn > 1
)
DELETE FROM notification_personne np
USING doublons d
WHERE np.id_notification = d.id_notification;

-- ── 3. Photos rattachées aux notifications / suivis dupliqués ──
WITH doublons AS (
    SELECT id_notification FROM (
        SELECT n.id_notification,
               row_number() OVER (
                   PARTITION BY n.id_titre_foncier,
                                n.id_parcelle,
                                n.date_notification,
                                COALESCE(n.date_convocation::text, ''),
                                COALESCE(n.informations_occupants, ''),
                                COALESCE(n.constats, ''),
                                COALESCE(n.doleances, ''),
                                COALESCE(n.actions_entreprises, ''),
                                COALESCE(n.statut, ''),
                                COALESCE((SELECT string_agg(np.id_personne::text, ',' ORDER BY np.id_personne)
                                          FROM notification_personne np
                                          WHERE np.id_notification = n.id_notification), '')
                   ORDER BY n.created_at, n.id_notification) AS rn
        FROM notification_occupation n
    ) t
    WHERE t.rn > 1
)
DELETE FROM photo p
USING doublons d
WHERE (p.entite_type = 'notification' AND p.entite_id = d.id_notification)
   OR (p.entite_type = 'suivi' AND p.entite_id IN
       (SELECT id_suivi FROM suivi_notification s WHERE s.id_notification = d.id_notification));

-- ── 4. Notifications dupliquées ──
WITH doublons AS (
    SELECT id_notification FROM (
        SELECT n.id_notification,
               row_number() OVER (
                   PARTITION BY n.id_titre_foncier,
                                n.id_parcelle,
                                n.date_notification,
                                COALESCE(n.date_convocation::text, ''),
                                COALESCE(n.informations_occupants, ''),
                                COALESCE(n.constats, ''),
                                COALESCE(n.doleances, ''),
                                COALESCE(n.actions_entreprises, ''),
                                COALESCE(n.statut, ''),
                                COALESCE((SELECT string_agg(np.id_personne::text, ',' ORDER BY np.id_personne)
                                          FROM notification_personne np
                                          WHERE np.id_notification = n.id_notification), '')
                   ORDER BY n.created_at, n.id_notification) AS rn
        FROM notification_occupation n
    ) t
    WHERE t.rn > 1
)
DELETE FROM notification_occupation n
USING doublons d
WHERE n.id_notification = d.id_notification;

-- ── 5. Avertissements dupliqués ──
WITH doublons AS (
    SELECT id_avertissement FROM (
        SELECT a.id_avertissement,
               row_number() OVER (
                   PARTITION BY a.id_titre_foncier,
                                a.id_parcelle,
                                a.date_avertissement,
                                a.id_personne,
                                COALESCE(a.informations_occupants, ''),
                                COALESCE(a.constats, ''),
                                COALESCE(a.actions_entreprises, ''),
                                COALESCE(a.a_faire, ''),
                                COALESCE(a.mission, '')
                   ORDER BY a.created_at, a.id_avertissement) AS rn
        FROM avertissement a
    ) t
    WHERE t.rn > 1
)
DELETE FROM photo p
USING doublons d
WHERE p.entite_type = 'avertissement' AND p.entite_id = d.id_avertissement;

WITH doublons AS (
    SELECT id_avertissement FROM (
        SELECT a.id_avertissement,
               row_number() OVER (
                   PARTITION BY a.id_titre_foncier,
                                a.id_parcelle,
                                a.date_avertissement,
                                a.id_personne,
                                COALESCE(a.informations_occupants, ''),
                                COALESCE(a.constats, ''),
                                COALESCE(a.actions_entreprises, ''),
                                COALESCE(a.a_faire, ''),
                                COALESCE(a.mission, '')
                   ORDER BY a.created_at, a.id_avertissement) AS rn
        FROM avertissement a
    ) t
    WHERE t.rn > 1
)
DELETE FROM avertissement a
USING doublons d
WHERE a.id_avertissement = d.id_avertissement;

-- ============================================================================
-- FIN DE LA MIGRATION V9
-- ============================================================================
