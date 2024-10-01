package bot;

import java.util.Arrays;

public class User {
    private long userId; // ID пользователя
    private String userLink; // Ссылка на пользователя
    private String[] activeAds; // Ссылки на активные объявления
    private int activeAdsCount; // Количество активных объявлений
    private int earnedMoney; // Количество заработанных рублей

    public User(long userId, String userLink) {
        this.userId = userId;
        this.userLink = userLink;
        this.activeAds = new String[0]; // Изначально нет активных объявлений
        this.activeAdsCount = 0;
        this.earnedMoney = 0;
    }

    public long getUserId() {
        return userId;
    }

    public String getUserLink() {
        return userLink;
    }

    public String[] getActiveAds() {
        return activeAds;
    }

    public int getActiveAdsCount() {
        return activeAdsCount;
    }

    public int getEarnedMoney() {
        return earnedMoney;
    }

    public void addAd(String adLink) {
        activeAds = Arrays.copyOf(activeAds, activeAds.length + 1);
        activeAds[activeAds.length - 1] = adLink;
        activeAdsCount++;
    }

    public void removeAd(String adLink) {
        activeAds = Arrays.stream(activeAds)
                .filter(ad -> !ad.equals(adLink))
                .toArray(String[]::new);
        activeAdsCount = activeAds.length;
    }

    public void addEarnings(int amount) {
        earnedMoney += amount;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", userLink='" + userLink + '\'' +
                ", activeAds=" + Arrays.toString(activeAds) +
                ", activeAdsCount=" + activeAdsCount +
                ", earnedMoney=" + earnedMoney +
                '}';
    }
}