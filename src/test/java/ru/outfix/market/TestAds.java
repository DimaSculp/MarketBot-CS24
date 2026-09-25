package ru.outfix.market;

import ru.outfix.market.ad.Ad;
import ru.outfix.market.ad.AdDraft;
import ru.outfix.market.ad.AdStatus;
import ru.outfix.market.geo.GeoPoint;

import java.util.List;

/** Заготовки объявлений и черновиков для тестов. */
public final class TestAds {

    private TestAds() {
    }

    public static AdDraft readyDraft(long userId, List<String> photos) {
        return new AdDraft(userId, AdDraft.Step.PHOTOS, "Кроссовки", "Почти новые", 4500, null, photos);
    }

    public static AdDraft draftAt(long userId, AdDraft.Step step) {
        return new AdDraft(userId, step, null, null, null, null, List.of());
    }

    public static Ad ad(long id, long userId, AdStatus status, Integer channelMessageId, List<String> photos) {
        return new Ad(id, userId, "Кроссовки", "Почти новые", 4500, null, null, status, channelMessageId, photos);
    }

    public static Ad adWithLocation(GeoPoint location, String address) {
        return new Ad(1, 1, "t", "d", 1, location, address, AdStatus.PENDING, null, List.of("p"));
    }
}
