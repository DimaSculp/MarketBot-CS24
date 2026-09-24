package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.ForwardMessage;

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
            List<Long> allUserIds = databaseHandler.getAllUserIds();

            for (Long userId : allUserIds) {
                ForwardMessage forwardMessage = new ForwardMessage(
                        userId,
                        channelPost.chat().id(),
                        channelPost.messageId()
                );
                bot.execute(forwardMessage);
            }
            System.out.println("Сообщение из Маркет Пуши переслано " + allUserIds.size() + " пользователям.");
        }
    }
}