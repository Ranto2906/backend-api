-- ============================================================================
-- MIGRATION V10 : Correction des doublons de notification restants
--
-- La migration V9 supprimait d'abord les enfants (notification_personne) des
-- notifications dupliquées : la signature de doublon (qui intègre la liste des
-- personnes liées) devenait alors différente et les parents dupliqués n'étaient
-- plus détectés (50 notifications au lieu de 25).
--
-- Correction ciblée et idempotente : on supprime une notification qui
--   - n'a plus AUCUN lien personne, ET
--   - correspond (titre + parcelle + date + contenu) à une notification
--     existante qui, elle, possède toujours ses liens personnes.
--
-- Sur une base saine ou déjà nettoyée, cette requête ne supprime rien.
-- ============================================================================

DELETE FROM notification_occupation n
WHERE NOT EXISTS (
        SELECT 1 FROM notification_personne np
        WHERE np.id_notification = n.id_notification
    )
  AND EXISTS (
        SELECT 1 FROM notification_occupation m
        WHERE m.id_notification <> n.id_notification
          AND m.id_titre_foncier = n.id_titre_foncier
          AND m.id_parcelle IS NOT DISTINCT FROM n.id_parcelle
          AND m.date_notification IS NOT DISTINCT FROM n.date_notification
          AND COALESCE(m.informations_occupants, '') = COALESCE(n.informations_occupants, '')
          AND COALESCE(m.constats, '') = COALESCE(n.constats, '')
          AND COALESCE(m.doleances, '') = COALESCE(n.doleances, '')
          AND COALESCE(m.actions_entreprises, '') = COALESCE(n.actions_entreprises, '')
          AND COALESCE(m.statut, '') = COALESCE(n.statut, '')
          AND EXISTS (
                SELECT 1 FROM notification_personne np2
                WHERE np2.id_notification = m.id_notification
              )
    );

-- ============================================================================
-- FIN DE LA MIGRATION V10
-- ============================================================================
