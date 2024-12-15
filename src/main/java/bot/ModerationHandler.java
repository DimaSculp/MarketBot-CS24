package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.CopyMessage;

import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModerationHandler {
    private final TelegramBot bot;
    private final DatabaseHandler databaseHandler;

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
                System.out.println("Проверенное объявление: " + originalText);
                System.out.println("Текст ответа: " + replyText);
                if (replyText != null) {
                    String userLink = extractUserLink(originalText);
                    long userId = databaseHandler.findUserIdByUserlink(userLink);
                    if (replyText.equalsIgnoreCase("approved")) {
                        long targetChannelId = -1002397946078L;
                        if (repliedMessage.mediaGroupId() != null) {
                            List<String> photos = parseFileId(originalText);
                            originalText = originalText.substring(0, originalText.indexOf('~')).trim();
                            System.out.println("Собранные фотографии: " + photos);
                            InputMediaPhoto[] media = new InputMediaPhoto[photos.size()];
                            for (int i = 0; i < photos.size(); i++) {
                                media[i] = new InputMediaPhoto(photos.get(i));
                                if (i == 0) {
                                    media[i].caption(originalText).parseMode(ParseMode.HTML);
                                }
                            }
                            bot.execute(new SendMediaGroup(userId, media));
                            bot.execute(new SendMediaGroup(targetChannelId, media));
                        } else {
                            originalText = originalText.substring(0, originalText.indexOf('~')).trim();
                            bot.execute(new CopyMessage(
                                    targetChannelId,
                                    repliedMessage.chat().id(),
                                    repliedMessage.messageId()
                            ).parseMode(ParseMode.HTML).caption(originalText));
                            bot.execute(new CopyMessage(
                                    userId,
                                    repliedMessage.chat().id(),
                                    repliedMessage.messageId()
                            ).parseMode(ParseMode.HTML).caption(originalText));
                        }
                        bot.execute(new SendMessage(userId, "Ваше объявление опубликовано!"));
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
