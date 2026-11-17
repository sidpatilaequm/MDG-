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
EOF
sudo chmod 600 /etc/mdg/mdg.env
sudo chown mdg:mdg /etc/mdg/mdg.env

# First release so `current` resolves before the first deploy's "Activate release" step
sudo mkdir -p /opt/mdg/releases/0
sudo ln -s /opt/mdg/releases/0 /opt/mdg/current
sudo chown -R mdg:mdg /opt/mdg
```

The user the CI job SSHes in as (`HOSTINGER_USER`) needs passwordless `sudo` for exactly:
`systemctl daemon-reload`, `systemctl enable mdg`, `systemctl restart mdg`, and
`install -m644 /tmp/mdg.service /etc/systemd/system/mdg.service` — e.g. via a
`/etc/sudoers.d/mdg-deploy` entry scoped to those commands.

Everything else (the systemd unit itself, each release's jar/tessdata, the symlink swap,
the health check, rollback) is handled by `.github/workflows/mdg-ci.yml` on every push to
`main`.
