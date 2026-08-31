POST   /api/auth/login              — Connexion JWT
POST   /api/auth/register           — Inscription
POST   /api/auth/refresh            — Rafraîchir token
POST   /api/auth/logout             — Déconnexion
GET    /api/auth/me                 — Utilisateur connecté
PUT    /api/auth/change-password    — Changer mot de passe

GET    /api/utilisateurs            — Liste paginée + recherche
POST   /api/utilisateurs            — Créer
PUT    /api/utilisateurs/{id}       — Modifier
DELETE /api/utilisateurs/{id}       — Supprimer
PATCH  /api/utilisateurs/{id}/activer — Activer/Désactiver
POST   /api/utilisateurs/{id}/reinitialiser-mot-de-passe
GET    /api/utilisateurs/{id}/roles — Lister rôles
POST   /api/utilisateurs/{id}/roles — Attribuer rôle
DELETE /api/utilisateurs/{id}/roles/{roleId} — Retirer rôle

GET    /api/roles                   — Liste rôles
POST   /api/roles                   — Créer rôle
GET    /api/roles/{id}/permissions  — Permissions du rôle
POST   /api/roles/{id}/permissions  — Attribuer permission
GET    /api/roles/modules           — Modules RBAC
GET    /api/roles/entites           — Entités RBAC
GET    /api/roles/actions           — Actions RBAC

GET    /api/admin/journal           — Journal connexions
POST   /api/admin/sessions/{id}/revoquer — Révoquer sessions