package bot;

import bot.Callbacks.AdCallback;
import bot.Commands.HelpCommand;
import bot.Commands.ProfileCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.pengrad.telegrambot.model.Chat;

import java.util.Map;
import com.pengrad.telegrambot.model.Location;
import static org.mockito.Mockito.*;

public class CallbackHandlersTest {

    private CallbackHandlers callbackHandlers;
    private TelegramBot bot;
    private CallbackQuery callbackQuery;
    private Message message;
    private Chat chat;

    @BeforeEach
    public void setUp() {
        bot = mock(TelegramBot.class);

        callbackQuery = mock(CallbackQuery.class);

        message = mock(Message.class);

        chat = mock(Chat.class);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);

        when(callbackQuery.message()).thenReturn(message);

        DatabaseHandler databaseHandler = mock(DatabaseHandler.class);
        callbackHandlers = new CallbackHandlers(databaseHandler);
    }

    @Test
    public void testHandleCallbackToCreate() {
        when(callbackQuery.data()).thenReturn("to_create");
        when(callbackQuery.message().chat().id()).thenReturn(123L);

        callbackHandlers.handleCallback(bot, callbackQuery, null);

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    public void testHandleCallbackToProfile() {
        when(callbackQuery.data()).thenReturn("to_profile");
        when(callbackQuery.message().chat().id()).thenReturn(123L);

        ProfileCommand profileCommand = mock(ProfileCommand.class);
        when(profileCommand.getContent()).thenReturn("Profile content");
        when(profileCommand.getKeyboard()).thenReturn(null);

        callbackHandlers.handleCallback(bot, callbackQuery, Map.of("/profile", profileCommand));

        verify(bot).execute(any(SendMessage.class));
    }


    @Test
    public void testHandleCallbackToHelp() {
        when(callbackQuery.data()).thenReturn("to_help");
        when(callbackQuery.message().chat().id()).thenReturn(123L);

        HelpCommand helpCommand = mock(HelpCommand.class);
        when(helpCommand.getContent()).thenReturn("Help content");
        when(helpCommand.getKeyboard()).thenReturn(null);

        callbackHandlers.handleCallback(bot, callbackQuery, Map.of("/help", helpCommand));

        verify(bot).execute(any(SendMessage.class));
    }


    @Test
    public void testHandleCallbackEndCreate() {
        when(callbackQuery.data()).thenReturn("end_create");
        when(callbackQuery.message().chat().id()).thenReturn(123L);

        callbackHandlers.handleCallback(bot, callbackQuery, null);

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    public void testHandleMessageTitle() {
        when(message.chat().id()).thenReturn(123L);
        when(message.text()).thenReturn("Test title");

        AdCallback adCallback = mock(AdCallback.class);
        when(adCallback.isTitleSet()).thenReturn(false);
        when(adCallback.setTitle(anyString())).thenReturn("Title set successfully");

        callbackHandlers.adCallbacks.put(123L, adCallback);

        callbackHandlers.handleMessage(bot, message);

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    public void testHandleMessageDescription() {
        when(message.chat().id()).thenReturn(123L);
        when(message.text()).thenReturn("Test description");

        AdCallback adCallback = mock(AdCallback.class);
        when(adCallback.isTitleSet()).thenReturn(true);
        when(adCallback.isDescriptionSet()).thenReturn(false);
        when(adCallback.setDescription(anyString())).thenReturn("Description set successfully");

        callbackHandlers.adCallbacks.put(123L, adCallback);

        callbackHandlers.handleMessage(bot, message);

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    public void testHandleMessagePrice() {
        when(message.chat().id()).thenReturn(123L);
        when(message.text()).thenReturn("500");

        AdCallback adCallback = mock(AdCallback.class);
        when(adCallback.isTitleSet()).thenReturn(true);
        when(adCallback.isDescriptionSet()).thenReturn(true);
        when(adCallback.isPriceSet()).thenReturn(false);
        when(adCallback.setPrice(anyInt())).thenReturn("Price set successfully");

        callbackHandlers.adCallbacks.put(123L, adCallback);

        callbackHandlers.handleMessage(bot, message);

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    public void testHandleMessageGeo() {
        when(message.chat().id()).thenReturn(123L);

        Location location = mock(Location.class);
        when(location.latitude()).thenReturn(55.0f);
        when(location.longitude()).thenReturn(37.0f);
        when(message.location()).thenReturn(location);

        AdCallback adCallback = mock(AdCallback.class);
        when(adCallback.checkGeo()).thenReturn(true);

        when(adCallback.setTitle("Test Title")).thenReturn("успешно");
        when(adCallback.setDescription("Test Description")).thenReturn("успешно");
        when(adCallback.setPrice(100)).thenReturn("успешно");

        callbackHandlers.adCallbacks.put(123L, adCallback);
    }
}
