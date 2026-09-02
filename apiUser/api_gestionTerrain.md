📍 /api/villes — VilleController
GET	/api/villes	Lister (pagination)
GET	/api/villes/all	Lister toutes (sans pagination)
GET	/api/villes/search?search=	Rechercher par nom
GET	/api/villes/{id}	Détail
POST	/api/villes	Créer
PUT	/api/villes/{id}	Modifier
DELETE	/api/villes/{id}	Supprimer


🏠 /api/proprietes — ProprieteController
GET	/api/proprietes	Lister (pagination)
GET	/api/proprietes/search?search=	Rechercher par nom
GET	/api/proprietes/ville/{idVille}	Lister par ville
GET	/api/proprietes/{id}	Détail
POST	/api/proprietes	Créer
PUT	/api/proprietes/{id}	Modifier
DELETE	/api/proprietes/{id}	Supprimer


📄 /api/titres-fonciers — TitreFoncierController
GET	/api/titres-fonciers	Lister (pagination)
GET	/api/titres-fonciers/{id}	Détail
GET	/api/titres-fonciers/numero/{numero}	Rechercher par numéro
POST	/api/titres-fonciers	Créer
PUT	/api/titres-fonciers/{id}	Modifier
DELETE	/api/titres-fonciers/{id}	Supprimer


💰 /api/prix-m2 — PrixM2Controller
GET	/api/prix-m2	Lister (pagination)
GET	/api/prix-m2/ville/{idVille}	Lister par ville
GET	/api/prix-m2/{id}	Détail
POST	/api/prix-m2	Créer
PUT	/api/prix-m2/{id}	Modifier
DELETE	/api/prix-m2/{id}	Supprimer


🗺️ /api/parcelles — ParcelleController
GET	/api/parcelles	Lister (pagination)
GET	/api/parcelles/titre-foncier/{idTitreFoncier}	Lister par titre foncier
GET	/api/parcelles/{id}	Détail
POST	/api/parcelles	Créer
PUT	/api/parcelles/{id}	Modifier
DELETE	/api/parcelles/{id}	Supprimer