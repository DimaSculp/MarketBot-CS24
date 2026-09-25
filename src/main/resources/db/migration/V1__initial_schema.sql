-- Пользователи бота. id совпадает с id личного чата с ботом.
CREATE TABLE users
(
    id         BIGINT PRIMARY KEY,
    username   TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Черновик объявления, который пользователь заполняет по шагам. Не больше одного на пользователя.
-- Хранится в БД, чтобы диалог переживал перезапуск бота при деплое.
CREATE TABLE ad_drafts
(
    user_id     BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    step        TEXT        NOT NULL DEFAULT 'TITLE'
        CHECK (step IN ('TITLE', 'DESCRIPTION', 'PRICE', 'LOCATION', 'PHOTOS')),
    title       TEXT,
    description TEXT,
    price       INTEGER,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    photo_ids   TEXT[]      NOT NULL DEFAULT '{}' CHECK (cardinality(photo_ids) <= 10),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Объявления. Жизненный цикл: PENDING → PUBLISHED | REJECTED; REJECTED → PUBLISHED; PUBLISHED → SOLD | REMOVED.
CREATE TABLE ads
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT      NOT NULL REFERENCES users (id),
    title              TEXT        NOT NULL CHECK (char_length(title) BETWEEN 1 AND 45),
    description        TEXT        NOT NULL CHECK (char_length(description) BETWEEN 1 AND 700),
    price              INTEGER     NOT NULL CHECK (price >= 0),
    latitude           DOUBLE PRECISION,
    longitude          DOUBLE PRECISION,
    address            TEXT,
    status             TEXT        NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PUBLISHED', 'REJECTED', 'SOLD', 'REMOVED')),
    rejection_reason   TEXT,
    -- Первое сообщение поста в канале объявлений (с подписью)
    channel_message_id INTEGER,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at       TIMESTAMPTZ,
    closed_at          TIMESTAMPTZ,
    CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CHECK (status IN ('PENDING', 'REJECTED') OR channel_message_id IS NOT NULL)
);

CREATE INDEX ads_user_status_idx ON ads (user_id, status);

CREATE TABLE ad_photos
(
    ad_id    BIGINT   NOT NULL REFERENCES ads (id) ON DELETE CASCADE,
    position SMALLINT NOT NULL CHECK (position BETWEEN 0 AND 9),
    file_id  TEXT     NOT NULL,
    PRIMARY KEY (ad_id, position)
);

-- Сообщения поста в канале модерации: модератор может ответить на любое фото альбома.
CREATE TABLE ad_moderation_messages
(
    chat_id    BIGINT  NOT NULL,
    message_id INTEGER NOT NULL,
    ad_id      BIGINT  NOT NULL REFERENCES ads (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, message_id)
);

CREATE INDEX ad_moderation_messages_ad_idx ON ad_moderation_messages (ad_id);
