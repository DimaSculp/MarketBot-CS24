package ru.outfix.market.db;

import ru.outfix.market.ad.Ad;
import ru.outfix.market.ad.AdDraft;
import ru.outfix.market.ad.AdStatus;
import ru.outfix.market.geo.GeoPoint;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Объявления, их фото и сообщения в канале модерации. */
public class AdRepository {

    private static final String SELECT_AD = """
            SELECT a.id, a.user_id, a.title, a.description, a.price, a.latitude, a.longitude, a.address,
                   a.status, a.channel_message_id,
                   ARRAY(SELECT p.file_id FROM ad_photos p WHERE p.ad_id = a.id ORDER BY p.position) AS photo_ids
            FROM ads a
            """;

    private final Jdbc jdbc;

    public AdRepository(DataSource dataSource) {
        this.jdbc = new Jdbc(dataSource);
    }

    /** Создаёт объявление со статусом PENDING из заполненного черновика. */
    public Ad create(AdDraft draft, String address) {
        long id = jdbc.inTransaction(conn -> {
            long adId;
            try (PreparedStatement st = conn.prepareStatement("""
                    INSERT INTO ads (user_id, title, description, price, latitude, longitude, address)
                    VALUES (?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
                st.setLong(1, draft.userId());
                st.setString(2, draft.title());
                st.setString(3, draft.description());
                st.setInt(4, draft.price());
                GeoPoint location = draft.location();
                if (location == null) {
                    st.setNull(5, Types.DOUBLE);
                    st.setNull(6, Types.DOUBLE);
                } else {
                    st.setDouble(5, location.latitude());
                    st.setDouble(6, location.longitude());
                }
                st.setString(7, address);
                st.executeUpdate();
                try (ResultSet keys = st.getGeneratedKeys()) {
                    keys.next();
                    adId = keys.getLong("id");
                }
            }
            try (PreparedStatement st = conn.prepareStatement(
                    "INSERT INTO ad_photos (ad_id, position, file_id) VALUES (?, ?, ?)")) {
                List<String> photos = draft.photoIds();
                for (int i = 0; i < photos.size(); i++) {
                    st.setLong(1, adId);
                    st.setShort(2, (short) i);
                    st.setString(3, photos.get(i));
                    st.addBatch();
                }
                st.executeBatch();
            }
            return adId;
        });
        return findById(id).orElseThrow();
    }

    public void delete(long adId) {
        jdbc.update("DELETE FROM ads WHERE id = ?", st -> st.setLong(1, adId));
    }

    public Optional<Ad> findById(long adId) {
        return jdbc.queryOne(SELECT_AD + "WHERE a.id = ?", st -> st.setLong(1, adId), AdRepository::map);
    }

    public void addModerationMessages(long adId, long chatId, List<Integer> messageIds) {
        jdbc.inTransaction(conn -> {
            try (PreparedStatement st = conn.prepareStatement(
                    "INSERT INTO ad_moderation_messages (chat_id, message_id, ad_id) VALUES (?, ?, ?)")) {
                for (int messageId : messageIds) {
                    st.setLong(1, chatId);
                    st.setInt(2, messageId);
                    st.setLong(3, adId);
                    st.addBatch();
                }
                st.executeBatch();
            }
            return null;
        });
    }

    /** Объявление, к которому относится сообщение в канале модерации (любое фото альбома). */
    public Optional<Ad> findByModerationMessage(long chatId, int messageId) {
        return jdbc.queryOne(SELECT_AD + """
                        JOIN ad_moderation_messages m ON m.ad_id = a.id
                        WHERE m.chat_id = ? AND m.message_id = ?""",
                st -> {
                    st.setLong(1, chatId);
                    st.setInt(2, messageId);
                },
                AdRepository::map);
    }

    /** @return {@code false}, если объявление уже не на модерации */
    public boolean markPublished(long adId, int channelMessageId) {
        return jdbc.update("""
                        UPDATE ads SET status = 'PUBLISHED', channel_message_id = ?, published_at = now(),
                                       rejection_reason = NULL
                        WHERE id = ? AND status IN ('PENDING', 'REJECTED')""",
                st -> {
                    st.setInt(1, channelMessageId);
                    st.setLong(2, adId);
                }) > 0;
    }

    /** @return {@code false}, если объявление уже не на модерации */
    public boolean markRejected(long adId, String reason) {
        return jdbc.update("""
                        UPDATE ads SET status = 'REJECTED', rejection_reason = ?
                        WHERE id = ? AND status IN ('PENDING', 'REJECTED')""",
                st -> {
                    st.setString(1, reason);
                    st.setLong(2, adId);
                }) > 0;
    }

    /** Опубликованные объявления пользователя в порядке публикации. */
    public List<Ad> findPublishedByUser(long userId) {
        return jdbc.query(SELECT_AD + "WHERE a.user_id = ? AND a.status = 'PUBLISHED' ORDER BY a.published_at, a.id",
                st -> st.setLong(1, userId), AdRepository::map);
    }

    /**
     * Снимает опубликованное объявление пользователя с публикации.
     *
     * @param status {@link AdStatus#SOLD} или {@link AdStatus#REMOVED}
     * @return {@code false}, если объявление не принадлежит пользователю или уже снято
     */
    public boolean close(long adId, long userId, AdStatus status) {
        if (status != AdStatus.SOLD && status != AdStatus.REMOVED) {
            throw new IllegalArgumentException("Недопустимый статус снятия: " + status);
        }
        return jdbc.update("""
                        UPDATE ads SET status = ?, closed_at = now()
                        WHERE id = ? AND user_id = ? AND status = 'PUBLISHED'""",
                st -> {
                    st.setString(1, status.name());
                    st.setLong(2, adId);
                    st.setLong(3, userId);
                }) > 0;
    }

    /** Количество объявлений по статусам (для метрик). */
    public Map<AdStatus, Long> countByStatus() {
        Map<AdStatus, Long> counts = new EnumMap<>(AdStatus.class);
        for (AdStatus status : AdStatus.values()) {
            counts.put(status, 0L);
        }
        jdbc.query("SELECT status, count(*) AS n FROM ads GROUP BY status", Jdbc.NO_PARAMS,
                rs -> counts.put(AdStatus.valueOf(rs.getString("status")), rs.getLong("n")));
        return counts;
    }

    private static Ad map(ResultSet rs) throws SQLException {
        Double lat = Jdbc.getDouble(rs, "latitude");
        Double lon = Jdbc.getDouble(rs, "longitude");
        return new Ad(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("price"),
                lat == null ? null : new GeoPoint(lat.floatValue(), lon.floatValue()),
                rs.getString("address"),
                AdStatus.valueOf(rs.getString("status")),
                Jdbc.getInteger(rs, "channel_message_id"),
                Jdbc.getStrings(rs, "photo_ids"));
    }
}
