package ru.outfix.market.ad;

import ru.outfix.market.geo.GeoPoint;

import java.util.List;

/**
 * Объявление.
 *
 * @param location         место встречи или {@code null}
 * @param address          адрес места встречи или {@code null}
 * @param channelMessageId первое сообщение поста в канале объявлений (у него подпись) или {@code null}, если не опубликовано
 */
public record Ad(
        long id,
        long userId,
        String title,
        String description,
        int price,
        GeoPoint location,
        String address,
        AdStatus status,
        Integer channelMessageId,
        List<String> photoIds
) {

    public boolean canBeModerated() {
        return status == AdStatus.PENDING || status == AdStatus.REJECTED;
    }
}
