CRUD Personnes (/api/personnes)

GET /api/personnes	Lister les personnes (pagination)
GET /api/personnes/search?search=...	Rechercher des personnes
GET /api/personnes/{id}	Détail d'une personne
POST /api/personnes	Créer une personne
PUT /api/personnes/{id}	Mettre à jour une personne
DELETE /api/personnes/{id}	Supprimer une personne
GET /api/personnes/role/{role}	Lister par rôle
GET /api/personnes/search/nom?nom=...	Rechercher par nom

2. Gestion des Notifications (/api/notifications)
GET /api/notifications	Lister les notifications
GET /api/notifications/search?search=...	Rechercher
GET /api/notifications/{id}	Détail d'une notification
POST /api/notifications	Créer avec personnes associées
PUT /api/notifications/{id}	Mettre à jour
DELETE /api/notifications/{id}	Supprimer
GET /api/notifications/statut/{statut}	Par statut
GET /api/notifications/annee/{annee}	Par année
GET /api/notifications/titre-foncier/{id}	Par titre foncier
GET /api/notifications/parcelle/{id}	Par parcelle
GET /api/notifications/stats	Statistiques

3. Suivi des Notifications (/api/notifications/{idNotification}/suivis)
GET /api/notifications/{id}/suivis	Lister les suivis
GET /api/notifications/{id}/suivis/{idSuivi}	Détail d'un suivi
POST /api/notifications/{id}/suivis	Créer un suivi
PUT /api/notifications/{id}/suivis/{idSuivi}	Mettre à jour
DELETE /api/notifications/{id}/suivis/{idSuivi}	Supprimer

4. Avertissements (/api/avertissements)
GET /api/avertissements	Lister les avertissements
GET /api/avertissements/search?search=...	Rechercher
GET /api/avertissements/{id}	Détail
POST /api/avertissements	Créer
PUT /api/avertissements/{id}	Mettre à jour
DELETE /api/avertissements/{id}	Supprimer
GET /api/avertissements/personne/{id}	Par personne
GET /api/avertissements/annee/{annee}	Par année
GET /api/avertissements/titre-foncier/{id}	Par titre foncier
GET /api/avertissements/parcelle/{id}	Par parcelle
GET /api/avertissements/stats	Statistiques