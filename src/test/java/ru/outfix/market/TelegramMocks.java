package ru.outfix.market;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Location;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.User;

import static org.mockito.Mockito.*;

/** Заготовки объектов Telegram для тестов. */
public final class TelegramMocks {

    private TelegramMocks() {
    }

    public static Message message(long chatId, String text) {
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(chatId);
        when(message.chat()).thenReturn(chat);
        when(message.text()).thenReturn(text);
        User from = user(chatId, "test_user");
        when(message.from()).thenReturn(from);
        return message;
    }

    public static Message photoMessage(long chatId, String fileId) {
        Message message = message(chatId, null);
        PhotoSize small = mock(PhotoSize.class);
        PhotoSize large = mock(PhotoSize.class);
        when(large.fileId()).thenReturn(fileId);
        when(message.photo()).thenReturn(new PhotoSize[]{small, large});
        return message;
    }

    public static Message locationMessage(long chatId, float latitude, float longitude) {
        Message message = message(chatId, null);
        Location location = mock(Location.class);
        when(location.latitude()).thenReturn(latitude);
        when(location.longitude()).thenReturn(longitude);
        when(message.location()).thenReturn(location);
        return message;
    }

    public static User user(long id, String username) {
        User user = mock(User.class);
        when(user.id()).thenReturn(id);
        when(user.username()).thenReturn(username);
        return user;
    }
}
