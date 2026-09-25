package ru.outfix.market.ad;

import ru.outfix.market.config.BotConfig;
import ru.outfix.market.db.UserAccount;
import ru.outfix.market.geo.GeoPoint;
import ru.outfix.market.util.Html;

import java.util.List;

/** Собирает HTML-тексты объявлений для каналов и сообщений бота. */
public class AdFormatter {

    private final BotConfig config;

    public AdFormatter(BotConfig config) {
        this.config = config;
    }

    /** Подпись поста с объявлением (для канала модерации и канала объявлений). */
    public String format(Ad ad, UserAccount seller) {
        StringBuilder sb = new StringBuilder()
                .append("<b>").append(Html.escape(ad.title())).append("</b>\n\n")
                .append("<i>").append(Html.escape(ad.description())).append("</i>\n\n")
                .append("<b>Цена: </b>").append(ad.price()).append(" руб.\n");

        GeoPoint location = ad.location();
        if (location != null) {
            String address = ad.address() != null ? ad.address() : location.toString();
            String geoLink = config.botLink() + "?start=" + location.toStartPayload();
            sb.append("<b>Место: </b>")
                    .append("<a href=\"").append(geoLink).append("\">")
                    .append("<i>").append(Html.escape(address)).append("</i></a>\n\n");
        }

        String sellerName = seller.username() != null ? seller.username() : "продавец";
        sb.append("<b>").append(Html.escape(sellerName)).append("</b>\n\n")
                .append("<a href=\"").append(seller.link()).append("\">контакт продавца</a>\n")
                .append("<a href=\"").append(config.botLink()).append("\">разместить объявление</a>");
        return sb.toString();
    }

    /** Подпись, которой заменяется объявление в канале после продажи. */
    public String soldCaption(UserAccount seller) {
        return "<b><i>SOLD SOLD SOLD</i></b>\n\n"
                + "<a href=\"" + seller.link() + "\">ПРОДАВЕЦ</a>\n"
                + "<a href=\"" + config.marketChannelLink() + "\">МАРКЕТ</a>\n\n";
    }

    /** Ссылка на пост объявления в канале. */
    public String channelLink(Ad ad) {
        return config.marketChannelLink() + "/" + ad.channelMessageId();
    }

    /** Нумерованный список опубликованных объявлений, или {@code null}, если их нет. */
    public String formatActiveAds(List<Ad> ads) {
        if (ads.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("<b>Список активных объявлений:</b>\n\n");
        for (int i = 0; i < ads.size(); i++) {
            Ad ad = ads.get(i);
            sb.append(i + 1).append(") <a href=\"").append(channelLink(ad)).append("\">")
                    .append(Html.escape(ad.title())).append("</a>\n");
        }
        return sb.toString();
    }
}
