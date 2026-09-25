package ru.outfix.market.db;

import ru.outfix.market.ad.AdDraft;
import ru.outfix.market.geo.GeoPoint;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.Optional;

/** Черновики объявлений: у пользователя не больше одного. */
public class AdDraftRepository {

    private static final String COLUMNS = "user_id, step, title, description, price, latitude, longitude, photo_ids";

    private final Jdbc jdbc;

    public AdDraftRepository(DataSource dataSource) {
        this.jdbc = new Jdbc(dataSource);
    }

    /** Начинает новый черновик, сбрасывая предыдущий. Пользователь должен существовать. */
    public void start(long userId) {
        jdbc.update("""
                        INSERT INTO ad_drafts (user_id) VALUES (?)
                        ON CONFLICT (user_id) DO UPDATE SET
                            step = 'TITLE', title = NULL, description = NULL, price = NULL,
                            latitude = NULL, longitude = NULL, photo_ids = '{}', updated_at = now()""",
                st -> st.setLong(1, userId));
    }

    public Optional<AdDraft> find(long userId) {
        return jdbc.queryOne("SELECT " + COLUMNS + " FROM ad_drafts WHERE user_id = ?",
                st -> st.setLong(1, userId),
                rs -> {
                    Double lat = Jdbc.getDouble(rs, "latitude");
                    Double lon = Jdbc.getDouble(rs, "longitude");
                    return new AdDraft(
                            rs.getLong("user_id"),
                            AdDraft.Step.valueOf(rs.getString("step")),
                            rs.getString("title"),
                            rs.getString("description"),
                            Jdbc.getInteger(rs, "price"),
                            lat == null ? null : new GeoPoint(lat.floatValue(), lon.floatValue()),
                            Jdbc.getStrings(rs, "photo_ids"));
                });
    }

    public boolean exists(long userId) {
        return jdbc.queryOne("SELECT 1 FROM ad_drafts WHERE user_id = ?",
                st -> st.setLong(1, userId), rs -> true).isPresent();
    }

    public void setTitle(long userId, String title) {
        jdbc.update("UPDATE ad_drafts SET title = ?, step = 'DESCRIPTION', updated_at = now() WHERE user_id = ?",
                st -> {
                    st.setString(1, title);
                    st.setLong(2, userId);
                });
    }

    public void setDescription(long userId, String description) {
        jdbc.update("UPDATE ad_drafts SET description = ?, step = 'PRICE', updated_at = now() WHERE user_id = ?",
                st -> {
                    st.setString(1, description);
                    st.setLong(2, userId);
                });
    }

    public void setPrice(long userId, int price) {
        jdbc.update("UPDATE ad_drafts SET price = ?, step = 'LOCATION', updated_at = now() WHERE user_id = ?",
                st -> {
                    st.setInt(1, price);
                    st.setLong(2, userId);
                });
    }

    /** Место встречи; {@code null} — пользователь отказался его указывать. */
    public void setLocation(long userId, GeoPoint location) {
        jdbc.update("""
                        UPDATE ad_drafts SET latitude = ?, longitude = ?, step = 'PHOTOS', updated_at = now()
                        WHERE user_id = ?""",
                st -> {
                    if (location == null) {
                        st.setNull(1, Types.DOUBLE);
                        st.setNull(2, Types.DOUBLE);
                    } else {
                        st.setDouble(1, location.latitude());
                        st.setDouble(2, location.longitude());
                    }
                    st.setLong(3, userId);
                });
    }

    /** @return {@code false}, если черновик не на шаге фото или фото уже 10 */
    public boolean addPhoto(long userId, String fileId) {
        return jdbc.update("""
                        UPDATE ad_drafts SET photo_ids = array_append(photo_ids, ?), updated_at = now()
                        WHERE user_id = ? AND step = 'PHOTOS' AND cardinality(photo_ids) < ?""",
                st -> {
                    st.setString(1, fileId);
                    st.setLong(2, userId);
                    st.setInt(3, AdDraft.MAX_PHOTOS);
                }) > 0;
    }

    public boolean delete(long userId) {
        return jdbc.update("DELETE FROM ad_drafts WHERE user_id = ?", st -> st.setLong(1, userId)) > 0;
    }
}
