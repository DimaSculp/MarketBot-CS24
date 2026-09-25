package ru.outfix.market.ad;

import org.junit.jupiter.api.Test;
import ru.outfix.market.TestAds;
import ru.outfix.market.TestConfig;
import ru.outfix.market.db.UserAccount;
import ru.outfix.market.geo.GeoPoint;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdFormatterTest {

    private static final UserAccount SELLER = new UserAccount(1, "test_user");

    private final AdFormatter formatter = new AdFormatter(TestConfig.config());

    @Test
    void formatsAdWithoutLocation() {
        Ad ad = new Ad(1, 1, "Моё объявление", "Описание объявления.", 1500, null, null, AdStatus.PENDING, null, List.of("p"));

        String expected = "<b>Моё объявление</b>\n\n"
                + "<i>Описание объявления.</i>\n\n"
                + "<b>Цена: </b>1500 руб.\n"
                + "<b>test_user</b>\n\n"
                + "<a href=\"https://t.me/test_user\">контакт продавца</a>\n"
                + "<a href=\"https://t.me/Market_OutFix_Bot\">разместить объявление</a>";
        assertEquals(expected, formatter.format(ad, SELLER));
    }

    @Test
    void formatsLocationWithAddressAndDeepLink() {
        Ad ad = TestAds.adWithLocation(new GeoPoint(56.5f, 60.25f), "Екатеринбург, улица Мира, 19");

        assertTrue(formatter.format(ad, SELLER).contains(
                "<b>Место: </b><a href=\"https://t.me/Market_OutFix_Bot?start=geo_56_500000_60_250000\">"
                        + "<i>Екатеринбург, улица Мира, 19</i></a>\n\n"));
    }

    @Test
    void fallsBackToCoordinatesWithoutAddress() {
        Ad ad = TestAds.adWithLocation(new GeoPoint(56.5f, 60.25f), null);
        assertTrue(formatter.format(ad, SELLER).contains("<i>56.500000, 60.250000</i>"));
    }

    @Test
    void escapesUserInput() {
        Ad ad = new Ad(1, 1, "Nike <3", "Цена & торг", 10, null, null, AdStatus.PENDING, null, List.of());

        String content = formatter.format(ad, SELLER);

        assertTrue(content.startsWith("<b>Nike &lt;3</b>"), content);
        assertTrue(content.contains("<i>Цена &amp; торг</i>"));
    }

    @Test
    void userWithoutUsernameGetsNeutralLabel() {
        Ad ad = TestAds.ad(1, 5, AdStatus.PENDING, null, List.of());
        String content = formatter.format(ad, new UserAccount(5, null));

        assertTrue(content.contains("<b>продавец</b>"));
        assertTrue(content.contains("<a href=\"tg://user?id=5\">контакт продавца</a>"));
    }

    @Test
    void formatsActiveAdsList() {
        assertNull(formatter.formatActiveAds(List.of()));
        String list = formatter.formatActiveAds(List.of(
                new Ad(1, 1, "Первое", "d", 1, null, null, AdStatus.PUBLISHED, 10, List.of()),
                new Ad(2, 1, "Второе & ко", "d", 1, null, null, AdStatus.PUBLISHED, 12, List.of())));
        assertEquals("<b>Список активных объявлений:</b>\n\n"
                + "1) <a href=\"https://t.me/OutFix_Market/10\">Первое</a>\n"
                + "2) <a href=\"https://t.me/OutFix_Market/12\">Второе &amp; ко</a>\n", list);
    }

    @Test
    void soldCaptionLinksSellerAndMarket() {
        assertEquals("<b><i>SOLD SOLD SOLD</i></b>\n\n"
                + "<a href=\"https://t.me/test_user\">ПРОДАВЕЦ</a>\n"
                + "<a href=\"https://t.me/OutFix_Market\">МАРКЕТ</a>\n\n", formatter.soldCaption(SELLER));
    }
}
