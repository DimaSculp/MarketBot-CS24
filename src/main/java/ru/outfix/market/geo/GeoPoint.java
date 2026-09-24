package ru.outfix.market.geo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

/**
 * Координаты места встречи и их кодирование в deep-link бота:
 * {@code https://t.me/<bot>?start=geo_<lat>_<latFrac>_<lon>_<lonFrac>}, дробная часть — 6 знаков.
 * Формат совместим со ссылками в уже опубликованных объявлениях.
 */
public record GeoPoint(float latitude, float longitude) {

    public static final String START_PREFIX = "geo_";

    public String toStartPayload() {
        return START_PREFIX + encode(latitude) + "_" + encode(longitude);
    }

    /** Разбирает payload команды {@code /start}, например {@code geo_56_838011_60_597465}. */
    public static Optional<GeoPoint> fromStartPayload(String payload) {
        if (payload == null || !payload.startsWith(START_PREFIX)) {
            return Optional.empty();
        }
        String[] parts = payload.substring(START_PREFIX.length()).split("_");
        if (parts.length != 4) {
            return Optional.empty();
        }
        try {
            float lat = Float.parseFloat(parts[0] + "." + parts[1]);
            float lon = Float.parseFloat(parts[2] + "." + parts[3]);
            return Optional.of(new GeoPoint(lat, lon));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.6f, %.6f", latitude, longitude);
    }

    private static String encode(float value) {
        // Знак пишем отдельно, иначе для -0.5 получилось бы "0_-500000"
        String sign = value < 0 ? "-" : "";
        // Float.toString даёт кратчайшее десятичное представление, без артефактов перевода в double
        BigDecimal abs = new BigDecimal(Float.toString(Math.abs(value))).setScale(6, RoundingMode.HALF_UP);
        BigDecimal whole = abs.setScale(0, RoundingMode.DOWN);
        int fraction = abs.subtract(whole).movePointRight(6).intValueExact();
        return String.format(Locale.ROOT, "%s%s_%06d", sign, whole.toPlainString(), fraction);
    }
}
