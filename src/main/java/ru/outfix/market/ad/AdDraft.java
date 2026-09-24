package ru.outfix.market.ad;

import ru.outfix.market.geo.GeoPoint;

import java.util.List;
import java.util.Optional;

/** Черновик объявления, который пользователь заполняет по шагам. */
public record AdDraft(
        long userId,
        Step step,
        String title,
        String description,
        Integer price,
        GeoPoint location,
        List<String> photoIds
) {

    public static final int MAX_TITLE_LENGTH = 45;
    public static final int MAX_DESCRIPTION_LENGTH = 700;
    public static final int MAX_PHOTOS = 10;

    public enum Step { TITLE, DESCRIPTION, PRICE, LOCATION, PHOTOS }

    public boolean hasPhotos() {
        return !photoIds.isEmpty();
    }

    /** @return текст ошибки, если название не подходит */
    public static Optional<String> validateTitle(String title) {
        if (title.isBlank()) {
            return Optional.of("Ошибка: название не может быть пустым.");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return Optional.of("Ошибка: название превышает " + MAX_TITLE_LENGTH + " символов.");
        }
        return Optional.empty();
    }

    public static Optional<String> validateDescription(String description) {
        if (description.isBlank()) {
            return Optional.of("Ошибка: описание не может быть пустым.");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            return Optional.of("Ошибка: описание превышает " + MAX_DESCRIPTION_LENGTH + " символов.");
        }
        return Optional.empty();
    }

    public static Optional<String> validatePrice(int price) {
        return price < 0 ? Optional.of("Ошибка: цена не может быть отрицательной.") : Optional.empty();
    }
}
