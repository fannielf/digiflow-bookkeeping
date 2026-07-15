# Deploying DigiFlow Books to Google Cloud (Cloud Run + Cloud SQL)

This guide takes the app from your laptop to a public HTTPS URL on Google Cloud.

**The shape of it:** your app runs as a container on **Cloud Run** (serverless — it
scales to zero when nobody's using it, so it's nearly free for a hobby project).
The database is a managed **Cloud SQL for PostgreSQL** instance. Cloud Run talks to
Cloud SQL over a secure socket, and the DB password lives in **Secret Manager**
rather than in your code.

```
Browser ──HTTPS──> Cloud Run (your container) ──socket──> Cloud SQL (Postgres)
                          │
                          └── reads DB_PASS from Secret Manager
```

You'll do this once from the terminal. Copy-paste the blocks in order and swap in
your own values where marked.

---

## What the code changes did

Before you deploy, here's what changed in the repo and why — worth understanding,
not just running.

- **`pom.xml`** — added `postgres-socket-factory`. This is Google's library that lets
  the Postgres JDBC driver connect to Cloud SQL without you managing IP allowlists or
  SSL certs. It's only exercised by the cloud profile.
- **`application.yml`** — `server.port` now reads `${PORT:8080}`. Cloud Run tells your
  app which port to listen on via the `PORT` env var; locally it still defaults to 8080.
- **`application-cloud.yml`** (new) — a Spring profile named `cloud`. It disables the
  docker-compose auto-start (no Docker in the container), points the datasource at
  Cloud SQL via the socket factory, turns on Thymeleaf caching, and reads DB name/user/
  password from env vars. We activate it by setting `SPRING_PROFILES_ACTIVE=cloud`.
- **`Dockerfile`** (new) — a two-stage build: stage 1 uses Maven+JDK 21 to build the
  JAR, stage 2 copies just the JAR onto a small JRE image and runs as a non-root user.
- **`.dockerignore` / `.gcloudignore`** — keep build junk out of the image/upload.
- **`docker-compose.yml`** — fixed the port mapping to `5435:5432` so local dev matches
  the `localhost:5435` URL in `application.yml`.

---

## 0. One-time prerequisites

Install the gcloud CLI and log in:

```bash
# macOS
brew install --cask google-cloud-sdk

gcloud auth login
```

Pick (or note) these values — you'll reuse them throughout. **Set them as shell
variables now** so the later commands just work:

```bash
export PROJECT_ID="digiflow-books-$RANDOM"   # must be globally unique; or use your own
export REGION="europe-north1"                # Finland region, low latency for you
export SERVICE="digiflow-books"              # Cloud Run service name
export DB_INSTANCE="digiflow-db"             # Cloud SQL instance name
export DB_NAME="digiflow_books"
export DB_USER="books"
```

Create the project and link billing (Cloud SQL requires a billing account, though
this setup stays in/near the free tier for hobby use):

```bash
gcloud projects create "$PROJECT_ID"
gcloud config set project "$PROJECT_ID"

# Find your billing account ID, then link it:
gcloud billing accounts list
gcloud billing projects link "$PROJECT_ID" --billing-account=XXXXXX-XXXXXX-XXXXXX
```

Enable the APIs you'll use:

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com
```

---

## 1. Create the Cloud SQL (PostgreSQL) instance

```bash
gcloud sql instances create "$DB_INSTANCE" \
  --database-version=POSTGRES_16 \
  --tier=db-f1-micro \
  --region="$REGION" \
  --storage-size=10GB
```

`db-f1-micro` is the smallest/cheapest tier — fine for this app. Creation takes a
few minutes.

Create the database and the app's DB user with a password:

```bash
gcloud sql databases create "$DB_NAME" --instance="$DB_INSTANCE"

# Generate a strong password and store it in a shell variable for the next steps.
export DB_PASS="$(openssl rand -base64 24)"

gcloud sql users create "$DB_USER" \
  --instance="$DB_INSTANCE" \
  --password="$DB_PASS"
```

Grab the instance connection name — the app needs it (format `project:region:instance`):

```bash
export INSTANCE_CONNECTION_NAME="$(gcloud sql instances describe "$DB_INSTANCE" \
  --format='value(connectionName)')"
echo "$INSTANCE_CONNECTION_NAME"
```

---

## 2. Store the DB password in Secret Manager

Never put the password in an env var flag or in code. Store it as a secret and let
Cloud Run mount it:

```bash
printf "%s" "$DB_PASS" | gcloud secrets create digiflow-db-pass --data-file=-
```

Cloud Run runs as your project's **compute service account** by default. Give that
account permission to read the secret:

```bash
export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
export RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

gcloud secrets add-iam-policy-binding digiflow-db-pass \
  --member="serviceAccount:${RUNTIME_SA}" \
  --role="roles/secretmanager.secretAccessor"

# The same service account needs to talk to Cloud SQL:
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${RUNTIME_SA}" \
  --role="roles/cloudsql.client"
```

---

## 3. Deploy to Cloud Run

This one command builds the container from your `Dockerfile` (via Cloud Build),
pushes it, and deploys it — wiring in the Cloud SQL connection, the profile, the DB
env vars, and the secret:

```bash
gcloud run deploy "$SERVICE" \
  --source . \
  --region="$REGION" \
  --allow-unauthenticated \
  --add-cloudsql-instances="$INSTANCE_CONNECTION_NAME" \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,INSTANCE_CONNECTION_NAME=${INSTANCE_CONNECTION_NAME},DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-secrets="DB_PASS=digiflow-db-pass:latest"
```

What the flags do:

- `--source .` — build straight from your code; no manual `docker build`/`push` needed.
- `--allow-unauthenticated` — makes the site publicly reachable. Remove this once you
  add Spring Security (README roadmap #5) if you want it private.
- `--add-cloudsql-instances` — attaches the Cloud SQL socket so the socket factory works.
- `--set-env-vars` — activates the `cloud` profile and feeds the non-secret DB values.
- `--set-secrets` — mounts the secret as the `DB_PASS` env var the app reads.

When it finishes, gcloud prints a **Service URL** like
`https://digiflow-books-xxxxx.a.run.app`. Open it — you should see the dashboard.

First load may be a few seconds while Hibernate creates the tables (`ddl-auto: update`).

---

## 4. Verify

```bash
# Tail the logs if anything looks off:
gcloud run services logs read "$SERVICE" --region="$REGION" --limit=50
```

Then in the app: **Asetukset** → fill in business details → **Asiakkaat** → add a
customer → **Laskut** → create an invoice. If that persists across a page reload,
the database wiring is good.

---

## Redeploying after code changes

Just rerun the deploy command — same line as step 3:

```bash
gcloud run deploy "$SERVICE" --source . --region="$REGION" \
  --allow-unauthenticated \
  --add-cloudsql-instances="$INSTANCE_CONNECTION_NAME" \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,INSTANCE_CONNECTION_NAME=${INSTANCE_CONNECTION_NAME},DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-secrets="DB_PASS=digiflow-db-pass:latest"
```

---

## Cost & cleanup

Cloud Run scales to zero, so it costs ~nothing when idle. The `db-f1-micro` Cloud SQL
instance is the main cost — a few euros a month, and **it bills even when idle**
because the DB is always on.

To stop the DB billing when you're not using it:

```bash
gcloud sql instances patch "$DB_INSTANCE" --activation-policy=NEVER   # stop
gcloud sql instances patch "$DB_INSTANCE" --activation-policy=ALWAYS  # start again
```

To tear everything down:

```bash
gcloud run services delete "$SERVICE" --region="$REGION"
gcloud sql instances delete "$DB_INSTANCE"
gcloud secrets delete digiflow-db-pass
# or nuke the whole project:
gcloud projects delete "$PROJECT_ID"
```

---

## Troubleshooting

- **App won't start, logs mention the datasource** — check that all four of
  `INSTANCE_CONNECTION_NAME`, `DB_NAME`, `DB_USER` (env vars) and `DB_PASS` (secret)
  are set on the service: `gcloud run services describe $SERVICE --region=$REGION`.
- **"permission denied" for Cloud SQL or the secret** — the two IAM bindings in step 2
  didn't apply to the runtime service account. Re-run them and redeploy.
- **Build fails** — run `mvn clean package -DskipTests` locally first; the Docker build
  runs the same steps.
- **Timeout on first request** — Hibernate is creating tables. Reload after a moment.

---

## Good next steps (from the README roadmap)

Deploying naturally sets up the next lessons:

1. **Flyway migrations** — once real data is in Cloud SQL, `ddl-auto: update` gets risky.
   Versioned migrations + `validate` is the professional pattern.
2. **Spring Security** — add a login, then drop `--allow-unauthenticated` or gate it,
   so your books aren't world-readable.
3. **CI** — move the `mvn test` + deploy into GitHub Actions so pushing to `main` deploys.
