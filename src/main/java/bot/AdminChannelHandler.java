package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.CopyMessage;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.List;

public class AdminChannelHandler {
    private final TelegramBot bot;
    private final DatabaseHandler databaseHandler;
    private static final long MARKET_PUSH_CHANNEL_ID = -1003207168611L;

    public AdminChannelHandler(TelegramBot bot, DatabaseHandler databaseHandler) {
        this.bot = bot;
        this.databaseHandler = databaseHandler;
    }

    public void handleChannelPost(Message channelPost) {
        long chatId = channelPost.chat().id();
        if (chatId == MARKET_PUSH_CHANNEL_ID) {
            System.out.println("Получено сообщение из канала Маркет Пуши.");
            List<Long> allUserIds = databaseHandler.getAllUserIds(); // Получаем ID всех пользователей

            for (Long userId : allUserIds) {
                // Копируем сообщение всем пользователям
                CopyMessage copyMessage = new CopyMessage(
                        userId,
                        channelPost.chat().id(),
                        channelPost.messageId()
                );
                bot.execute(copyMessage);
            }
            System.out.println("Сообщение из Маркет Пуши переслано " + allUserIds.size() + " пользователям.");
        }
    }
}