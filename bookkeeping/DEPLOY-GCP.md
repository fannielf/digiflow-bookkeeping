# Deploying DigiFlow Books to Google Cloud (Cloud Run + Cloud SQL)

This guide takes the app from your laptop to a public HTTPS URL, and covers how to
update and redeploy afterwards.

**The shape of it:** your app runs as a container on **Cloud Run** (serverless — it
scales to zero when idle, so it's nearly free for a hobby project). The database is a
managed **Cloud SQL for PostgreSQL** instance. Cloud Run talks to Cloud SQL over a
secure socket, and the DB password lives in **Secret Manager**, not in your code.

```
Browser ──HTTPS──> Cloud Run (your container) ──socket──> Cloud SQL (Postgres)
                          │
                          └── reads DB_PASS from Secret Manager
```

**Where you run these commands:** your Mac's **Terminal** app, from inside the project
folder. The only thing to install locally is the `gcloud` CLI — Cloud Build compiles
the code in the cloud using the `Dockerfile`, so you don't need Maven, a JDK, or Docker
on your machine.

---

## What the code changes did

Worth understanding, not just running:

- **`pom.xml`** — added `postgres-socket-factory`, Google's library that lets the app
  reach Cloud SQL without managing IPs or SSL certs. Only used by the cloud profile.
- **`application.yml`** — `server.port` reads `${PORT:8080}`; Cloud Run sets `PORT`.
- **`application-cloud.yml`** (new) — a Spring profile named `cloud`. Points the
  datasource at Cloud SQL, disables docker-compose auto-start, enables Thymeleaf
  caching, and reads DB name/user/password from env vars. Activated with
  `SPRING_PROFILES_ACTIVE=cloud`.
- **`Dockerfile`** (new) — two-stage build: Maven+JDK 21 builds the JAR, then a small
  JRE 21 image runs it as a non-root user.
- **`.dockerignore` / `.gcloudignore`** — keep build junk out of the image/upload.
- **`docker-compose.yml`** — fixed the local port mapping to `5435:5432`.

---

# Part 1 — First-time deployment

Run steps 1–8 in **one Terminal session**. The `export` variables live only in that
session — if you close it, re-run the `export` lines (steps 3, and the ones in 6–7 that
capture values) before deploying again.

## 1. Install the CLI and log in

```bash
brew install --cask google-cloud-sdk
gcloud auth login
```

## 2. Go to your project folder

```bash
cd /Users/fanni.vesanen/projects/digiflow-bookkeeping/bookkeeping
```

## 3. Set your variables

```bash
export PROJECT_ID="digiflow-books-$RANDOM"   # must be globally unique
export REGION="europe-north1"
export SERVICE="digiflow-books"
export DB_INSTANCE="digiflow-db"
export DB_NAME="digiflow_books"
export DB_USER="books"
```

## 4. Create the project + link billing

```bash
gcloud projects create "$PROJECT_ID"
gcloud config set project "$PROJECT_ID"

gcloud billing accounts list
gcloud billing projects link "$PROJECT_ID" --billing-account=XXXXXX-XXXXXX-XXXXXX
```

Billing is required for Cloud SQL. New accounts get free credits.

## 5. Enable the APIs

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com
```

## 6. Create the database (takes a few minutes)

```bash
gcloud sql instances create "$DB_INSTANCE" \
  --database-version=POSTGRES_16 \
  --tier=db-f1-micro \
  --region="$REGION" \
  --storage-size=10GB

gcloud sql databases create "$DB_NAME" --instance="$DB_INSTANCE"

export DB_PASS="$(openssl rand -base64 24)"
gcloud sql users create "$DB_USER" --instance="$DB_INSTANCE" --password="$DB_PASS"

export INSTANCE_CONNECTION_NAME="$(gcloud sql instances describe "$DB_INSTANCE" --format='value(connectionName)')"
echo "$INSTANCE_CONNECTION_NAME"
```

## 7. Store the password as a secret + grant access

```bash
printf "%s" "$DB_PASS" | gcloud secrets create digiflow-db-pass --data-file=-

export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
export RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

gcloud secrets add-iam-policy-binding digiflow-db-pass \
  --member="serviceAccount:${RUNTIME_SA}" --role="roles/secretmanager.secretAccessor"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${RUNTIME_SA}" --role="roles/cloudsql.client"
```

## 8. Deploy 🚀

```bash
gcloud run deploy "$SERVICE" \
  --source . \
  --region="$REGION" \
  --allow-unauthenticated \
  --add-cloudsql-instances="$INSTANCE_CONNECTION_NAME" \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,INSTANCE_CONNECTION_NAME=${INSTANCE_CONNECTION_NAME},DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-secrets="DB_PASS=digiflow-db-pass:latest"
```

When it finishes it prints a **Service URL** (`https://digiflow-books-xxxxx.a.run.app`).
Open it — the first load takes a few seconds while Hibernate creates the tables.

---

# Part 2 — Updating the code and redeploying

This is the part you'll use most. **Here's the key point:** Cloud Run remembers the
config from your first deploy (the env vars, secret, and Cloud SQL attachment). So after
a code change you do **not** repeat the big command — you just point it at your updated
code again:

```bash
cd /Users/fanni.vesanen/projects/digiflow-bookkeeping/bookkeeping

gcloud run deploy digiflow-books --source . --region=europe-north1
```

That one line uploads your current code, rebuilds the container in the cloud, and rolls
out a new **revision**. If the build succeeds, traffic switches to it automatically; if
it fails, your old revision keeps serving — so a broken build never takes the site down.

The typical loop:

```bash
# 1. make your code changes and commit them (good habit)
git add -A && git commit -m "Add receipt attachments"

# 2. redeploy
gcloud run deploy digiflow-books --source . --region=europe-north1

# 3. open the URL and check it, or tail logs (see below)
```

**When you DO need the longer command again:** only if you're *changing configuration* —
e.g. adding a new env var or a new secret. To add/update a single env var without wiping
the others, use `--update-env-vars` (not `--set-env-vars`, which replaces them all):

```bash
gcloud run services update digiflow-books --region=europe-north1 \
  --update-env-vars="SOME_NEW_VAR=value"
```

---

# Part 3 — Useful commands cheat-sheet

Set these once per Terminal session so the commands below are short:

```bash
export SERVICE="digiflow-books"
export REGION="europe-north1"
export DB_INSTANCE="digiflow-db"
```

### See what's happening

```bash
# The public URL of your service
gcloud run services describe "$SERVICE" --region="$REGION" --format='value(status.url)'

# Live logs (add --limit=50 for the last 50 lines instead of a stream)
gcloud run services logs read "$SERVICE" --region="$REGION" --limit=50

# Full service config: env vars, secrets, image, revision
gcloud run services describe "$SERVICE" --region="$REGION"
```

### Revisions and rollback

Every deploy creates a revision. If a new one misbehaves, roll straight back:

```bash
# List revisions (newest first)
gcloud run revisions list --service="$SERVICE" --region="$REGION"

# Send 100% of traffic back to a known-good revision
gcloud run services update-traffic "$SERVICE" --region="$REGION" \
  --to-revisions=digiflow-books-00002-abc=100
```

### Secrets

```bash
# View the current DB password
gcloud secrets versions access latest --secret=digiflow-db-pass

# Rotate it: set a new DB password AND store it as a new secret version
NEW_PASS="$(openssl rand -base64 24)"
gcloud sql users set-password books --instance="$DB_INSTANCE" --password="$NEW_PASS"
printf "%s" "$NEW_PASS" | gcloud secrets versions add digiflow-db-pass --data-file=-
gcloud run deploy "$SERVICE" --source . --region="$REGION"   # redeploy to pick it up
```

### Connect to the database directly

```bash
# Opens a psql prompt to your Cloud SQL instance (asks for the password)
gcloud sql connect "$DB_INSTANCE" --user=books --database=digiflow_books
```

### Cost control

Cloud Run scales to zero (≈free when idle). The `db-f1-micro` Cloud SQL instance is the
main cost — a few euros/month, and **it bills even when idle** because the DB stays on.

```bash
# Pause the DB when you're not using it
gcloud sql instances patch "$DB_INSTANCE" --activation-policy=NEVER

# Start it again
gcloud sql instances patch "$DB_INSTANCE" --activation-policy=ALWAYS
```

### Tear everything down

```bash
gcloud run services delete "$SERVICE" --region="$REGION"
gcloud sql instances delete "$DB_INSTANCE"
gcloud secrets delete digiflow-db-pass
# or delete the whole project:
gcloud projects delete "$PROJECT_ID"
```

---

# Troubleshooting

- **App won't start, logs mention the datasource** — confirm the service has all four of
  `INSTANCE_CONNECTION_NAME`, `DB_NAME`, `DB_USER` (env vars) and `DB_PASS` (secret):
  `gcloud run services describe $SERVICE --region=$REGION`.
- **"permission denied" for Cloud SQL or the secret** — re-run the two IAM bindings in
  step 7, then redeploy.
- **Build fails** — read the Cloud Build output in the deploy log; the error is usually
  a compile error in your change.
- **Timeout on first request** — Hibernate is creating tables; reload after a moment.

---

# Good next steps (from the README roadmap)

1. **Flyway migrations** — once real data lives in Cloud SQL, `ddl-auto: update` gets
   risky. Versioned migrations + `validate` is the professional pattern.
2. **Spring Security** — add a login, then drop `--allow-unauthenticated` so your books
   aren't world-readable.
3. **CI/CD with GitHub Actions** — move the test + deploy into a workflow so pushing to
   `main` deploys automatically. This is the natural upgrade from redeploying by hand.
