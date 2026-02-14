# Carpooling API

API REST de covoiturage développée avec Spring Boot et PostgreSQL.

## 🚀 Technologies

- **Backend:** Spring Boot 4.x, Java 21
- **Base de données:** PostgreSQL 16
- **Conteneurisation:** Docker & Docker Compose
- **Hébergement:** Render

## 📦 Installation locale

### Prérequis
- Docker Desktop
- Java 21

### Démarrage
```bash
# Cloner le repository
git clone https://github.com/aarnow/carpooling.git
cd carpooling

# Démarrer les conteneurs
docker-compose up -d --build

# Vérifier le statut
docker-compose ps
```

### Arrêt
```bash
docker-compose down
```
___
# Todo : 
- lister les dépendances
- indiquer l'emplacement des diagrammes
- parler de la couverture de tests avec Jacoco
- parler de CI/CD avec Render