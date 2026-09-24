package ru.outfix.market.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoPointTest {

    @Test
    void encodesInLegacyFormat() {
        assertEquals("geo_56_500000_60_250000", new GeoPoint(56.5f, 60.25f).toStartPayload());
    }

    @Test
    void encodedPayloadDecodesToSamePoint() {
        GeoPoint original = new GeoPoint(56.838011f, 60.597465f);
        assertEquals(original, GeoPoint.fromStartPayload(original.toStartPayload()).orElseThrow());
    }

    @Test
    void decodesLegacyPayload() {
        GeoPoint point = GeoPoint.fromStartPayload("geo_56_838011_60_597465").orElseThrow();
        assertEquals(56.838011f, point.latitude(), 1e-6);
        assertEquals(60.597465f, point.longitude(), 1e-6);
    }

    @Test
    void negativeCoordinatesRoundTrip() {
        GeoPoint original = new GeoPoint(-0.5f, -73.985428f);
        GeoPoint decoded = GeoPoint.fromStartPayload(original.toStartPayload()).orElseThrow();
        assertEquals(original.latitude(), decoded.latitude(), 1e-5);
        assertEquals(original.longitude(), decoded.longitude(), 1e-5);
    }

    @Test
    void rejectsMalformedPayload() {
        assertTrue(GeoPoint.fromStartPayload("geo_1_2_3").isEmpty());
        assertTrue(GeoPoint.fromStartPayload("geo_a_b_c_d").isEmpty());
        assertTrue(GeoPoint.fromStartPayload("promo").isEmpty());
        assertTrue(GeoPoint.fromStartPayload(null).isEmpty());
    }

    @Test
    void shortensLongAddress() {
        assertEquals("Екатеринбург, улица Мира, 19",
                YandexGeocoder.shorten("Россия, Свердловская область, Екатеринбург, улица Мира, 19"));
        assertEquals("Россия, Екатеринбург", YandexGeocoder.shorten("Россия, Екатеринбург"));
    }
}
