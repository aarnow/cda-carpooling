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

# 🛠️ Qualité & Sécurité du projet

## 📊 Couverture de tests — JaCoCo

Les tests unitaires et d'intégration sont mesurés automatiquement par **JaCoCo** à chaque build.

Pour générer le rapport de couverture :

```bash
./mvnw clean verify
```

Le rapport HTML est disponible dans :

```
target/site/jacoco/index.html
```

---

## 📖 Documentation API — Swagger

La documentation interactive de l'API est générée automatiquement par **Springdoc OpenAPI**.

Une fois l'application démarrée, elle est accessible à :

```
http://localhost:8080/swagger-ui.html
```

La spec OpenAPI brute (utilisée notamment par ZAP) est disponible à :

```
http://localhost:8080/v3/api-docs
```

---

## 🔒 Analyse de sécurité — OWASP ZAP

**ZAP** (Zed Attack Proxy) effectue un scan automatisé des vulnérabilités de l'API en simulant des attaques HTTP externes.

### Prérequis

L'application doit être démarrée :

```bash
docker compose up -d postgres app
```

### Lancer le scan

```bash
docker compose run --rm zap
```

Le scan analyse les 109 endpoints exposés et génère deux rapports dans le dossier `zap-reports/` :

- `zap-report.html` — rapport lisible dans un navigateur
- `zap-report.json` — rapport brut au format JSON

---

## 🔍 Analyse de code — SonarQube

**SonarQube** analyse statiquement le code source pour détecter les bugs, vulnérabilités et mauvaises pratiques.

### Démarrer l'instance SonarQube

```bash
docker compose up sonarqube -d
```

L'interface est disponible à `http://localhost:9000` (identifiants par défaut : `admin` / `admin`).

### Lancer une analyse

**Sur Windows :**

```bash
.\mvnw.cmd clean verify sonar:sonar "-Dsonar.projectKey=carpooling-api" "-Dsonar.projectName=carpooling-api" "-Dsonar.host.url=http://localhost:9000" "-Dsonar.token=VOTRE_TOKEN"
```

**Sur Mac/Linux :**

```bash
./mvnw clean verify sonar:sonar \
  -Dsonar.projectKey=carpooling-api \
  -Dsonar.projectName=carpooling-api \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=VOTRE_TOKEN
```

> Le token est à générer depuis l'interface SonarQube : **My Account → Security → Generate Token**

___
# Todo : 
- lister les dépendances
- indiquer l'emplacement des diagrammes
- parler de CI/CD avec Render