package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.CopyMessage;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.response.MessageIdResponse;
import com.pengrad.telegrambot.response.MessagesResponse;

import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ModerationHandler {
    private final TelegramBot bot;
    private final DatabaseHandler databaseHandler;
    private static final String TARGET_CHANNEL_USERNAME = "OutFix_Market"; // Имя канала без @
    private static final long TARGET_CHANNEL_ID = -1003223929393L; // ID канала для размещения

    public ModerationHandler(TelegramBot bot, DatabaseHandler databaseHandler) {
        this.bot = bot;
        this.databaseHandler = databaseHandler;
    }

    protected List<String> parseFileId(String text) {
        List<String> fileIds = new ArrayList<>();
        Pattern pattern = Pattern.compile("~\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            fileIds.add(matcher.group(1));
        }
        return fileIds;
    }

    public void handleUpdate(Update update) {
        if (update.channelPost() != null) {
            Message channelPost = update.channelPost();
            if (channelPost.replyToMessage() != null) {
                Message repliedMessage = channelPost.replyToMessage();
                String replyText = channelPost.text();
                String originalText = repliedMessage.caption();
                System.out.println("Получено сообщение в канале модерации, ответ: " + replyText);

                if (replyText != null) {
                    String userLink = extractUserLink(originalText);
                    long userId = databaseHandler.findUserIdByUserlink(userLink);

                    if (replyText.equalsIgnoreCase("approved")) {
                        String originalTextTrimmed = originalText.substring(0, originalText.indexOf('~')).trim();
                        String messageLink = null;

                        List<String> fileIds = parseFileId(originalText);

                        if (!fileIds.isEmpty()) {
                            List<InputMediaPhoto> media = new ArrayList<>();
                            for (int i = 0; i < fileIds.size(); i++) {
                                InputMediaPhoto photo = new InputMediaPhoto(fileIds.get(i));
                                if (i == 0) {
                                    photo.caption(originalTextTrimmed);
                                    photo.parseMode(ParseMode.HTML);
                                }
                                media.add(photo);
                            }
                            MessagesResponse response = bot.execute(new SendMediaGroup(TARGET_CHANNEL_ID, media.toArray(new InputMediaPhoto[0])));

                            if (response.isOk() && response.messages().length > 0) {
                                int messageId = response.messages()[0].messageId();
                                messageLink = String.format("https://t.me/%s/%d", TARGET_CHANNEL_USERNAME, messageId);
                            }
                        } else {
                            SendResponse response = bot.execute(new SendMessage(TARGET_CHANNEL_ID, originalTextTrimmed)
                                    .parseMode(ParseMode.HTML));

                            if (response.isOk()) {
                                int messageId = response.message().messageId();
                                messageLink = String.format("https://t.me/%s/%d", TARGET_CHANNEL_USERNAME, messageId);
                            }
                        }

                        if (messageLink != null) {
                            databaseHandler.addAdToUser(userId, originalTextTrimmed);

                            SendMessage sendMessage = new SendMessage(userId,
                                    "Ваше объявление опубликовано в канале t.me/OutFix_Market!\n" +
                                            "Теперь оно доступно по <a href=\"" + messageLink + "\">ссылке</a>");
                            sendMessage = sendMessage.parseMode(ParseMode.HTML);
                            bot.execute(sendMessage);

                            System.out.println("Сообщение опубликовано. Ссылка: " + messageLink);
                        } else {
                            bot.execute(new SendMessage(userId, "Ваше объявление опубликовано, но не удалось получить ссылку."));
                            System.out.println("Сообщение опубликовано, но ссылка не получена.");
                        }
                    } else {
                        if (userLink != null && userId != 0) {
                            bot.execute(new SendMessage(userId, "Ваше объявление отклонено.\nПричина: " + replyText));
                            System.out.println("Пользователь уведомлен об отклонении.");
                        } else {
                            System.out.println("Не удалось уведомить пользователя об отклонении. userlink: " + userLink + ", userId: " + userId);
                        }
                    }
                }
            }
        }
    }


    protected String extractUserLink(String text) {
        System.out.println("Полученный текст: " + text);

        Pattern pattern = Pattern.compile("https://t.me/\\S+");
        Matcher matcher = pattern.matcher(text);

        String lastFoundLink = null;
        while (matcher.find()) {
            lastFoundLink = matcher.group();
        }


        if (lastFoundLink != null && !lastFoundLink.contains("geo_")) {
            System.out.println("Найдена ссылка: " + lastFoundLink);
            return lastFoundLink;
        }


        System.out.println("Ссылка пользователя не найдена.");
        return null;
    }

}