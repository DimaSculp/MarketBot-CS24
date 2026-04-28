# MarketBot-CS24

[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](#)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](#)
[![Telegram Bot API](https://img.shields.io/badge/Telegram_Bot_API-2CA5E0?style=flat-square&logo=telegram&logoColor=white)](#)
[![Status](https://img.shields.io/badge/статус-продакшен-brightgreen?style=flat-square)](#)

Маркетплейс б/у велосипедных запчастей на базе Telegram-бота.  
5–10 объявлений в день, живой продакшен с модерацией.

---

## Проблема и решение

Рынок б/у велозапчастей раздроблен: люди продают в случайных чатах, без фотографий нормального качества и без геолокации.  
MarketBot-CS24 — структурированная площадка внутри Telegram: продавец заполняет карточку через бота, модератор одобряет, объявление публикуется в канале.

---

## Архитектура

```
Продавец → [Бот приёма] → [Канал модерации] → (одобрение) → [Публичный канал объявлений]
                                                ↓ (отклонение)
                                           Уведомление продавцу
```

**Компоненты:**

| Компонент | Роль |
|-----------|------|
| Telegram-бот | Приём объявлений, диалог с продавцом, уведомления |
| Канал модерации | Приватный канал, куда падают объявления на проверку |
| Публичный канал | Витрина: одобренные объявления с фото и локацией |
| Yandex Geocoder | Определение города/района по адресу продавца |

**Жизненный цикл объявления:**
1. Продавец пишет боту и отвечает на вопросы (название, фото, цена, адрес)
2. Бот создаёт карточку и отправляет в канал модерации
3. Модератор нажимает «Одобрить» или «Отклонить»
4. При одобрении — карточка публикуется в публичный канал

---

## Стек

[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](#)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](#)
[![Telegram Bot API](https://img.shields.io/badge/Telegram_Bot_API-2CA5E0?style=flat-square&logo=telegram&logoColor=white)](#)
[![Yandex Geocoder](https://img.shields.io/badge/Yandex_Geocoder-FF0000?style=flat-square&logo=yandex&logoColor=white)](#)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](#)

---

## Скриншоты

> Вставьте скриншоты в папку `docs/screenshots/` и раскомментируйте строки ниже.

<!-- ![Диалог с ботом](docs/screenshots/bot_dialog.png) -->
<!-- *Процесс подачи объявления* -->

<!-- ![Канал модерации](docs/screenshots/moderation_channel.png) -->
<!-- *Карточка объявления с кнопками одобрения* -->

<!-- ![Публичный канал](docs/screenshots/public_channel.png) -->
<!-- *Опубликованное объявление в публичном канале* -->

---

## Запуск

### Переменные окружения

Создайте файл `.env` или передайте переменные в окружение:

```env
BOT_TOKEN=your_telegram_bot_token
MODERATION_CHANNEL_ID=-100xxxxxxxxxx
PUBLIC_CHANNEL_ID=-100xxxxxxxxxx
DB_HOST=localhost
DB_PORT=3306
DB_NAME=marketbot
DB_USER=root
DB_PASSWORD=secret
YANDEX_GEOCODER_KEY=your_yandex_api_key
```

### Локальный запуск

```bash
# Собрать
mvn clean package -DskipTests

# Запустить
java -jar target/MarketBot-CS24.jar
```

### Схема БД

```bash
# Накатить миграции (если используется Liquibase/Flyway)
mvn flyway:migrate
```

---

## Статус

**В продакшене** — 5–10 объявлений в день, активная модерация.
