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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class ModerationHandler {
    private final TelegramBot bot;
    private final DatabaseHandler databaseHandler;
    private static final String TARGET_CHANNEL_USERNAME = "KB2024CHANNEL";
    private static final long TARGET_CHANNEL_ID = -1002397946078L;

    public ModerationHandler(TelegramBot bot, DatabaseHandler databaseHandler) {
        this.bot = bot;
        this.databaseHandler = databaseHandler;
    }

    private List<String> parseFileId(String text) {
        List<String> fileIds = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return fileIds;
        }
        int tildeIndex = text.indexOf("~");
        if (tildeIndex == -1) {
            return fileIds;
        }
        String fileIdPart = text.substring(tildeIndex + 1).trim();
        if (fileIdPart.startsWith("[") && fileIdPart.endsWith("]")) {
            fileIdPart = fileIdPart.substring(1, fileIdPart.length() - 1);
            String[] ids = fileIdPart.split(",");
            for (String id : ids) {
                fileIds.add(id.trim());
            }
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
                //System.out.println("Проверенное объявление: " + originalText);
                //System.out.println("Текст ответа: " + replyText);
                if (replyText != null) {
                    String userLink = extractUserLink(originalText);
                    long userId = databaseHandler.findUserIdByUserlink(userLink);
                    if (replyText.equalsIgnoreCase("approved")) {
                        String originalTextTrimmed = originalText.substring(0, originalText.indexOf('~')).trim();
                        String messageLink = null;
                        if (repliedMessage.mediaGroupId() != null) {
                            List<String> photos = parseFileId(originalText);
                            //System.out.println("Собранные фотографии: " + photos);
                            InputMediaPhoto[] media = new InputMediaPhoto[photos.size()];
                            for (int i = 0; i < photos.size(); i++) {
                                media[i] = new InputMediaPhoto(photos.get(i));
                                if (i == 0) {
                                    media[i].caption(originalTextTrimmed).parseMode(ParseMode.HTML);
                                }
                            }
                            SendMediaGroup sendMediaGroup = new SendMediaGroup(TARGET_CHANNEL_ID, media);
                            MessagesResponse response = bot.execute(sendMediaGroup);
                            if (response.isOk()) {
                                Message[] sentMessage = response.messages();
                                int messageId = sentMessage[0].messageId();
                                messageLink = "https://t.me/" + TARGET_CHANNEL_USERNAME + "/" + messageId;
                                System.out.println("Ссылка на сообщение: " + messageLink);
                            }
                            bot.execute(new SendMediaGroup(userId, media));
                        } else {
                            CopyMessage copyToChannel = new CopyMessage(
                                    TARGET_CHANNEL_ID,
                                    repliedMessage.chat().id(),
                                    repliedMessage.messageId()
                            ).parseMode(ParseMode.HTML).caption(originalTextTrimmed);

                            MessageIdResponse response = bot.execute(copyToChannel);
                            if (response.isOk()) {
                                int messageId = response.messageId();
                                messageLink = "https://t.me/" + TARGET_CHANNEL_USERNAME + "/" + messageId;
                                System.out.println("Ссылка на сообщение: " + messageLink);
                            }

                            CopyMessage copyToUser = new CopyMessage(
                                    userId,
                                    repliedMessage.chat().id(),
                                    repliedMessage.messageId()
                            ).parseMode(ParseMode.HTML).caption(originalTextTrimmed);
                            bot.execute(copyToUser);
                        }

                        if (messageLink != null) {
                            System.out.println(databaseHandler.getConnection());
                            databaseHandler.addAdToUser(userId, messageLink);
                            System.out.println(userId);
                            SendMessage sendMessage = new SendMessage(userId, "Ваше объявление опубликовано!\nТеперь оно" +
                                    " доступно по <a href=\"" + messageLink + "\">ссылке</a>");
                            sendMessage = sendMessage.parseMode(ParseMode.HTML);
                            bot.execute(sendMessage);

                            System.out.println("Сообщение опубликовано. Ссылка: " + messageLink);
                        } else {
                            bot.execute(new SendMessage(userId, "Ваше объявление опубликовано, но не удалось получить ссылку."));
                            System.out.println("Сообщение опубликовано, но ссылка не получена.");
                        }
                    } else {
                        System.out.println(userLink);
                        if (userLink != null) {
                            if (userId != 0) {
                                bot.execute(new SendMessage(userId, "Ваше объявление отклонено.\nПричина: " + replyText));
                                System.out.println("Пользователь уведомлен.");
                            } else {
                                System.out.println("Пользователь с userlink " + userLink + " не найден в базе данных.");
                            }
                        } else {
                            System.out.println("Userlink не найден в тексте сообщения.");
                        }
                    }
                }
            }
        }
    }



    private String extractUserLink(String text) {
        System.out.println("Полученный текст: " + text);
        Pattern pattern = Pattern.compile("https://t.me/\\S+");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String found = matcher.group();
            System.out.println("Найдена ссылка: " + found);
            return found;
        }

        System.out.println("Ссылка не найдена.");
        return null;
    }

}
