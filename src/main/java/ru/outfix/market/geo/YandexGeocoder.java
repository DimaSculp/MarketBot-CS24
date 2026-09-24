package ru.outfix.market.geo;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/** Обратное геокодирование координат в адрес через HTTP Геокодер Яндекса. */
public class YandexGeocoder {

    private static final Logger log = LoggerFactory.getLogger(YandexGeocoder.class);
    private static final String ENDPOINT = "https://geocode-maps.yandex.ru/1.x/";

    private final String apiKey;
    private final HttpClient http;

    public YandexGeocoder(String apiKey) {
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * Возвращает короткий адрес вида «город, улица, дом».
     * Если ключ API не задан или запрос не удался, возвращает пустой Optional.
     */
    public Optional<String> findAddress(GeoPoint point) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String geocode = String.format(Locale.ROOT, "%f,%f", point.longitude(), point.latitude());
        URI uri = URI.create(ENDPOINT
                + "?apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&geocode=" + URLEncoder.encode(geocode, StandardCharsets.UTF_8)
                + "&format=json");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Геокодер вернул HTTP {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }
            return Optional.of(shorten(parseAddress(response.body())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Не удалось получить адрес для {}", point, e);
            return Optional.empty();
        }
    }

    static String parseAddress(String json) {
        return new JSONObject(json)
                .getJSONObject("response")
                .getJSONObject("GeoObjectCollection")
                .getJSONArray("featureMember")
                .getJSONObject(0)
                .getJSONObject("GeoObject")
                .getJSONObject("metaDataProperty")
                .getJSONObject("GeocoderMetaData")
                .getString("text");
    }

    /** «Россия, Свердловская область, Екатеринбург, улица Мира, 19» → «Екатеринбург, улица Мира, 19». */
    static String shorten(String address) {
        String[] parts = address.split(",\\s*");
        if (parts.length >= 5) {
            int n = parts.length;
            return parts[n - 3] + ", " + parts[n - 2] + ", " + parts[n - 1];
        }
        return address;
    }
}
