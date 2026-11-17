# MDG deploy — one-time server bootstrap

Done once, by hand, on the Hosticare server before the first CI deploy. CI never creates
these — `/etc/mdg/mdg.env` in particular must never be generated or echoed by a workflow.

```bash
sudo useradd --system --no-create-home --shell /usr/sbin/nologin mdg
sudo mkdir -p /opt/mdg/releases /etc/mdg
sudo chown -R mdg:mdg /opt/mdg

sudo tee /etc/mdg/mdg.env > /dev/null <<'EOF'
DB_URL=jdbc:mysql://localhost:3306/mdg?createDatabaseIfNotExist=true
DB_USERNAME=...
DB_PASSWORD=...
ATTESTR_AUTH_TOKEN=...
RAPIDAPI_KEY=...
BUSINESS_CARD_SCANNER_API_KEY=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
SPRING_SECURITY_PASSWORD=...
# Required - signs one-click approve/reject email links. The app will not start without it.
WORKFLOW_ACTION_TOKEN_SECRET=...
# Optional - supplier-onboarding features degrade gracefully without these:
MICROVISTA_TOKEN_ID=...
MICROVISTA_TOKEN_SECRET=...
APP_PUBLIC_BASE_URL=https://your-real-public-domain
SUPPLIER_ONBOARDING_OWNING_ADMIN_EMAIL=...
EOF
sudo chmod 600 /etc/mdg/mdg.env
sudo chown mdg:mdg /etc/mdg/mdg.env

# First release so `current` resolves before the first deploy's "Activate release" step
sudo mkdir -p /opt/mdg/releases/0
sudo ln -s /opt/mdg/releases/0 /opt/mdg/current
sudo chown -R mdg:mdg /opt/mdg
```

The user the CI job SSHes in as (`HOSTINGER_USER`) needs passwordless `sudo` for exactly:
`systemctl daemon-reload`, `systemctl enable mdg`, `systemctl restart mdg`,
`install -m644 /tmp/mdg.service /etc/systemd/system/mdg.service`, and
`chown -R mdg:mdg /opt/mdg/releases/*` (each new release directory is created by this SSH
user, not by `mdg`, but the app itself needs to write inside its own working directory at
runtime - e.g. `FileUploadService`'s `upload-dir`) — e.g. via a `/etc/sudoers.d/mdg-deploy`
entry scoped to those commands. (If this user is already `root`, as on a single-admin
Hosticare VPS, all of this is moot - root's own `sudo` never needs that scoping.)

Everything else (the systemd unit itself, each release's jar/tessdata, the symlink swap,
the health check, rollback) is handled by `.github/workflows/mdg-ci.yml` on every push to
`main`.
