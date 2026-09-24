package ru.outfix.market.db;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/** Пользователи бота. */
public class UserRepository {

    private final Jdbc jdbc;

    public UserRepository(DataSource dataSource) {
        this.jdbc = new Jdbc(dataSource);
    }

    /**
     * Создаёт пользователя или обновляет его username.
     *
     * @return {@code true}, если пользователь создан
     */
    public boolean upsert(long id, String username) {
        return jdbc.queryOne("""
                        INSERT INTO users (id, username) VALUES (?, ?)
                        ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username
                        RETURNING (xmax = 0) AS inserted""",
                st -> {
                    st.setLong(1, id);
                    st.setString(2, username);
                },
                rs -> rs.getBoolean("inserted")).orElseThrow();
    }

    public Optional<UserAccount> findById(long id) {
        return jdbc.queryOne("SELECT id, username FROM users WHERE id = ?",
                st -> st.setLong(1, id),
                rs -> new UserAccount(rs.getLong("id"), rs.getString("username")));
    }

    /** Число опубликованных объявлений и сумма цен проданных. */
    public UserStats stats(long id) {
        return jdbc.queryOne("""
                        SELECT count(*) FILTER (WHERE status = 'PUBLISHED')     AS active,
                               coalesce(sum(price) FILTER (WHERE status = 'SOLD'), 0) AS earned
                        FROM ads WHERE user_id = ?""",
                st -> st.setLong(1, id),
                rs -> new UserStats(rs.getInt("active"), rs.getLong("earned"))).orElseThrow();
    }

    public List<Long> findAllIds() {
        return jdbc.query("SELECT id FROM users ORDER BY id", Jdbc.NO_PARAMS, rs -> rs.getLong("id"));
    }

    public long count() {
        return jdbc.queryOne("SELECT count(*) FROM users", Jdbc.NO_PARAMS, rs -> rs.getLong(1)).orElseThrow();
    }
}
