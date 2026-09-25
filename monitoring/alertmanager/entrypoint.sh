#!/bin/sh
# Собирает конфиг Alertmanager из переменных окружения: Alertmanager не подставляет их сам.
# Алерты уходят в Telegram через того же бота; без ALERT_CHAT_ID они видны только в веб-интерфейсе Prometheus.
set -eu

config=/tmp/alertmanager.yml

if [ -n "${ALERT_CHAT_ID:-}" ]; then
  printf '%s' "$BOT_TOKEN" > /tmp/bot_token
  cat > "$config" <<YAML
route:
  receiver: telegram
  group_by: [alertname]
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
receivers:
  - name: telegram
    telegram_configs:
      - bot_token_file: /tmp/bot_token
        chat_id: ${ALERT_CHAT_ID}
        parse_mode: HTML
        send_resolved: true
YAML
else
  echo "ALERT_CHAT_ID не задан: уведомления в Telegram отключены" >&2
  cat > "$config" <<YAML
route:
  receiver: none
receivers:
  - name: none
YAML
fi

exec /bin/alertmanager --config.file="$config" --storage.path=/alertmanager "$@"
