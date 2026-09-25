package ru.outfix.market.telegram;

/**
 * Значения {@code callback_data} инлайн-кнопок. Менять строки нельзя:
 * кнопки в уже отправленных сообщениях продолжают присылать старые значения.
 */
public final class CallbackData {

    public static final String CREATE_AD = "to_create";
    public static final String PROFILE = "to_profile";
    public static final String HELP = "to_help";
    public static final String MY_ADS = "to_ads";
    public static final String REMOVE_AD = "remove_ads";
    public static final String FINISH_AD = "end_create";
    public static final String CANCEL_AD = "stop_creating";
    public static final String LOCATION_YES = "yes_geo";
    public static final String LOCATION_NO = "no_geo";
    /** Префиксы с id объявления: {@code sold_ad_42}, {@code unsold_ad_42}. */
    public static final String SOLD_PREFIX = "sold_ad_";
    public static final String UNSOLD_PREFIX = "unsold_ad_";

    private CallbackData() {
    }
}
