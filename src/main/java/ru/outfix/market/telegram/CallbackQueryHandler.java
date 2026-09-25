package ru.outfix.market.telegram;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import ru.outfix.market.ad.AdCreationFlow;
import ru.outfix.market.ad.AdFormatter;
import ru.outfix.market.ad.AdRemovalFlow;
import ru.outfix.market.command.CommandContext;
import ru.outfix.market.command.HelpCommand;
import ru.outfix.market.command.ProfileCommand;
import ru.outfix.market.db.AdRepository;

import static ru.outfix.market.telegram.CallbackData.*;

/** Обработка нажатий на инлайн-кнопки. */
public class CallbackQueryHandler {

    private final Messenger messenger;
    private final AdRepository ads;
    private final AdFormatter formatter;
    private final AdCreationFlow adCreation;
    private final AdRemovalFlow adRemoval;
    private final ProfileCommand profileCommand;
    private final HelpCommand helpCommand;

    public CallbackQueryHandler(Messenger messenger, AdRepository ads, AdFormatter formatter,
                                AdCreationFlow adCreation, AdRemovalFlow adRemoval,
                                ProfileCommand profileCommand, HelpCommand helpCommand) {
        this.messenger = messenger;
        this.ads = ads;
        this.formatter = formatter;
        this.adCreation = adCreation;
        this.adRemoval = adRemoval;
        this.profileCommand = profileCommand;
        this.helpCommand = helpCommand;
    }

    public void handle(CallbackQuery query) {
        try {
            dispatch(query);
        } finally {
            // Без ответа у кнопки бесконечно крутится индикатор загрузки
            messenger.execute(new AnswerCallbackQuery(query.id()));
        }
    }

    private void dispatch(CallbackQuery query) {
        String data = query.data();
        if (data == null) {
            return;
        }
        long chatId = chatId(query);
        CommandContext context = new CommandContext(chatId, query.from().username());

        if (data.startsWith(SOLD_PREFIX) || data.startsWith(UNSOLD_PREFIX)) {
            boolean sold = data.startsWith(SOLD_PREFIX);
            String adId = data.substring((sold ? SOLD_PREFIX : UNSOLD_PREFIX).length());
            try {
                adRemoval.confirm(chatId, Long.parseLong(adId), sold);
            } catch (NumberFormatException ignored) {
                // кнопка с некорректными данными — игнорируем
            }
            return;
        }

        switch (data) {
            case CREATE_AD -> adCreation.start(chatId, context.username());
            case CANCEL_AD -> adCreation.cancel(chatId);
            case FINISH_AD -> adCreation.finish(chatId);
            case LOCATION_YES -> adCreation.onLocationAnswer(chatId, true);
            case LOCATION_NO -> adCreation.onLocationAnswer(chatId, false);
            case PROFILE -> messenger.send(chatId, profileCommand.reply(context), profileCommand.keyboard());
            case HELP -> messenger.send(chatId, helpCommand.reply(context));
            case MY_ADS -> showActiveAds(chatId);
            case REMOVE_AD -> adRemoval.start(chatId);
            default -> {
                // неизвестная кнопка из старой версии бота
            }
        }
    }

    private void showActiveAds(long chatId) {
        String list = formatter.formatActiveAds(ads.findPublishedByUser(chatId));
        if (list == null) {
            messenger.send(chatId, "У вас нет активных объявлений. Вы можете создать новое, нажав кнопку ниже.", Keyboards.menu());
        } else {
            messenger.sendHtml(chatId, list, Keyboards.myAds());
        }
    }

    private static long chatId(CallbackQuery query) {
        if (query.message() != null && query.message().chat() != null) {
            return query.message().chat().id();
        }
        return query.from().id();
    }
}
