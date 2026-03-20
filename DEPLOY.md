# DEPLOY.md — Cloud Run Deployment Guide

## Project structure

| Environment | GCP Project | Cloud Run Service | Config file | Deploy script |
|---|---|---|---|---|
| Dev | `fitzenio-debug` | `fitzenio-api-dev` | `cloud-run-config.dev.yaml` | `./deploy-dev.sh` |
| Production | `fitzenio` | `fitzenio-api-prod` | `cloud-run-config.yaml` | `./deploy.sh` |

Secrets are stored in **Secret Manager** in each GCP project and referenced by name in the YAML config.
The Cloud Run service account must have `roles/secretmanager.secretAccessor` for each secret it reads.

---

## Regular deploy

```bash
# Dev
./deploy-dev.sh

# Production
./deploy.sh
```

Both scripts: build with Jib → push image to GCR → `gcloud run services replace <config>.yaml`.

> **Important:** `gcloud run services replace` uses the YAML as the full desired state — any env vars set
> manually in the Console will be wiped on the next deploy. Always make env changes in the YAML, not the UI.

---

## Adding a new API key / secret

Do this checklist for **each environment** you're adding the key to.

### Step 1 — Create the secret in Secret Manager

```bash
# Dev (fitzenio-debug)
echo -n "your-actual-key-value" | gcloud secrets create MY_NEW_KEY \
  --data-file=- \
  --project=fitzenio-debug

# Production (fitzenio)
echo -n "your-actual-key-value" | gcloud secrets create MY_NEW_KEY \
  --data-file=- \
  --project=fitzenio
```

To update an existing secret's value (add a new version):
```bash
echo -n "new-value" | gcloud secrets versions add MY_NEW_KEY \
  --data-file=- \
  --project=fitzenio-debug
```

### Step 2 — Grant the service account access

Edit `grant-secrets.sh` and add one line per new secret:
```bash
gcloud secrets add-iam-policy-binding MY_NEW_KEY \
  --member="$SA" \
  --role="roles/secretmanager.secretAccessor" \
  --project="$PROJECT"
```

> `grant-secrets.sh` currently targets `fitzenio-debug` (dev). For production, run the equivalent
> command manually with `--project=fitzenio` and the prod service account (see below).

Then run:
```bash
./grant-secrets.sh
```

> Safe to re-run — IAM bindings are idempotent.

### Step 3 — Add the env entry to the Cloud Run YAML

**Dev** (`cloud-run-config.dev.yaml`):
```yaml
- name: MY_NEW_KEY
  valueFrom:
    secretKeyRef:
      name: MY_NEW_KEY
      key: latest
```

**Production** (`cloud-run-config.yaml`) — same snippet.

For plain (non-secret) env vars:
```yaml
- name: MY_FEATURE_FLAG
  value: "true"
```

### Step 4 — Add to `.env.example`

```
MY_NEW_KEY=your_value_here
```

### Step 5 — Deploy

```bash
./deploy-dev.sh   # dev
./deploy.sh       # production
```

---

## Current secrets

| Secret name | Dev (`fitzenio-debug`) | Prod (`fitzenio`) |
|---|---|---|
| `FATSECRET_CLIENT_ID` | yes | yes |
| `FATSECRET_CLIENT_SECRET` | yes | yes |
| `USDA_API_KEY` | yes | yes |
| `OPENAI_API_KEY` | yes | yes |
| `GEMINI_API_KEY` | yes | yes |

---

## Production — first-time setup checklist

If standing up production from scratch in the `fitzenio` GCP project:

```bash
# 1. Authenticate
gcloud auth login
gcloud auth configure-docker

# 2. Enable required APIs
gcloud services enable run.googleapis.com secretmanager.googleapis.com \
  containerregistry.googleapis.com --project=fitzenio

# 3. Create all secrets (run once per secret)
echo -n "VALUE" | gcloud secrets create FATSECRET_CLIENT_ID   --data-file=- --project=fitzenio
echo -n "VALUE" | gcloud secrets create FATSECRET_CLIENT_SECRET --data-file=- --project=fitzenio
echo -n "VALUE" | gcloud secrets create USDA_API_KEY           --data-file=- --project=fitzenio
echo -n "VALUE" | gcloud secrets create OPENAI_API_KEY         --data-file=- --project=fitzenio
echo -n "VALUE" | gcloud secrets create GEMINI_API_KEY         --data-file=- --project=fitzenio

# 4. Find the production compute service account
gcloud iam service-accounts list --project=fitzenio
# Typically: <project-number>-compute@developer.gserviceaccount.com

# 5. Grant secret access (replace SA with the account found above)
SA="serviceAccount:PROD_SA@developer.gserviceaccount.com"
for SECRET in FATSECRET_CLIENT_ID FATSECRET_CLIENT_SECRET USDA_API_KEY OPENAI_API_KEY GEMINI_API_KEY; do
  gcloud secrets add-iam-policy-binding $SECRET \
    --member="$SA" --role="roles/secretmanager.secretAccessor" --project=fitzenio
done

# 6. Deploy
./deploy.sh
```

---

## Troubleshooting

**Service crashes on startup with `Missing GEMINI_API_KEY` (or similar)**
- The secret exists in Secret Manager but the service account doesn't have access → run the IAM grant step
- The env entry is missing from the YAML → add it and redeploy (manual Console edits are overwritten on deploy)

**`gcloud run services replace` fails with permission error**
- Make sure you're authenticated: `gcloud auth login`
- Make sure you're targeting the right project: `--project=fitzenio-debug` or `--project=fitzenio`

**Image push fails**
- Docker Desktop must be running
- Run `gcloud auth configure-docker` if you haven't yet
