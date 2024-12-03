package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.CopyMessage;
import com.pengrad.telegrambot.request.ForwardMessage;
import com.pengrad.telegrambot.request.SendMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModerationHandler {
    private final TelegramBot bot;
    private final DatabaseHandler databaseHandler;

    public ModerationHandler(TelegramBot bot, DatabaseHandler databaseHandler) {
        this.bot = bot;
        this.databaseHandler = databaseHandler;
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
                    if (replyText.equalsIgnoreCase("approved")) {
                        long targetChannelId = -1002397946078L;
                        long chatId = repliedMessage.chat().id();

                        bot.execute(new CopyMessage(targetChannelId, chatId, repliedMessage.messageId()));
                        System.out.println("Сообщение переслано в другой канал.");
                    } else {
                        String userLink = extractUserLink(originalText);
                        System.out.println(userLink);
                        if (userLink != null) {
                            long userId = findUserIdByUserlink(userLink);
                            if (userId != 0) {
                                bot.execute(new SendMessage(userId, "Ваше объявление отклонено. Причина: " + replyText));
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



    private long findUserIdByUserlink(String userlink) {
        String query = "SELECT user_id FROM public.users WHERE user_link = ?";
        try (Connection connection = databaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userlink);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
