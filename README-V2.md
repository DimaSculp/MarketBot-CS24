# OutFix Market Bot

## Локальный запуск

Нужен только Docker с Compose: бот собирается внутри контейнера, Java и Maven устанавливать не требуется.

### 1. Настройка

```bash
cp .env.example .env
```

Заполните все значения в `.env`. Бот должен быть администратором каналов модерации, объявлений и рассылки.

Для локальной проверки используйте отдельного тестового бота и тестовые каналы. Если запустить локально токен боевого бота, два экземпляра будут конфликтовать за обновления от Telegram.

### 2. Запуск

```bash
docker compose up -d --build --wait
```

Поднимаются бот, PostgreSQL, Prometheus и Alertmanager. Флаг `--wait` дожидается, пока бот станет `healthy`.

### 3. Проверка

```bash
docker compose ps              # у всех контейнеров статус Up, у bot и postgres — (healthy)
docker compose logs -f bot     # логи бота
```

- Веб-интерфейс Prometheus (графики и алерты): http://localhost:9090
- База данных: `localhost:5432`, логин и пароль — `POSTGRES_USER` / `POSTGRES_PASSWORD` из `.env`

### Частые команды

```bash
docker compose up -d --build bot           # пересобрать бота после изменений в коде
docker compose up -d --force-recreate bot  # перезапустить бота после правки .env
docker compose down                        # остановить (данные сохраняются)
docker compose down -v                     # остановить и удалить данные БД и метрик
```

### Тесты

Тесты тоже запускаются в Docker, включая интеграционные на настоящем PostgreSQL:

```bash
docker run --rm --add-host=host.docker.internal:host-gateway \
  -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -v "$PWD":/app -v outfix-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-25 mvn -B test
```

### Запуск без Docker

Нужен JDK 25. Базу можно оставить в Docker, а бота запускать из IDE: `DB_URL` в `.env` уже указывает на `localhost:5432`.

```bash
docker compose up -d postgres
```

Главный класс — `ru.outfix.market.Application`.
