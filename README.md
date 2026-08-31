# 🏛️ API Patrimoine Foncier SEIMAD

API Backend Spring Boot pour la gestion du patrimoine foncier.

## 🚀 Prérequis

- **Java 17** (ou supérieur)
- **Maven 3.8+**
- **PostgreSQL 14+**
- **IDE** : IntelliJ IDEA, Eclipse ou VS Code

## 📦 Installation

```bash
cd backend-api

# Installer les dépendances
./mvnw clean install
```

## ⚙️ Configuration Base de données

1. Créer la base PostgreSQL :
```sql
CREATE DATABASE patrimoine_db;
```

2. Modifier les paramètres dans `src/main/resources/application.properties` :
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/patrimoine_db
spring.datasource.username=votre_utilisateur
spring.datasource.password=votre_mot_de_passe
```

## 🏃 Démarrer l'application

```bash
# Development
./mvnw spring-boot:run

# Ou avec Maven
mvn spring-boot:run
```

L'application sera disponible sur : `http://localhost:8080`

## 📡 Endpoints

| Méthode | URL | Description | Authentification |
|---------|-----|-------------|-----------------|
| GET | `/api/public/health` | Santé de l'API | Non requise |
| GET | `/actuator/health` | Health Check Spring | Non requise |

## 🔐 Authentification

- **Utilisateur par défaut** : `admin` / `admin`
- Les endpoints non publics nécessitent une authentification Basic Auth

## 📁 Structure du Projet

```
backend-api/
├── src/
│   ├── main/
│   │   ├── java/com/seimad/patrimoine/
│   │   │   ├── PatrimoineApplication.java   # Classe principale
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java      # Configuration sécurité
│   │   │   ├── controller/                  # Contrôleurs REST
│   │   │   ├── model/                       # Entités JPA
│   │   │   ├── repository/                  # Repositories
│   │   │   └── service/                     # Services métier
│   │   └── resources/
│   │       └── application.properties       # Configuration
│   └── test/                                # Tests
├── pom.xml                                  # Dépendances Maven
└── README.md
```

## 🛠️ Technologies

- **Spring Boot 3.3.x**
- **Spring Web** (REST API)
- **Spring Data JPA** (Hibernate)
- **Spring Security** (Authentification)
- **PostgreSQL** (Base de données)
- **Lombok** (Réduction boilerplate)
- **Maven** (Gestion des dépendances)

## 📝 Prochaines Étapes

1. Créer les entités JPA (Parcelle, Proprietaire, etc.)
2. Créer lesRepositories Spring Data
3. Développer les services métier
4. Implémenter les contrôleurs REST
5. Ajouter les tests unitaires et d'intégration
