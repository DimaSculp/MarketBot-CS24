package bot.Commands;

import bot.Keyboards;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class AuthorsCommand implements BotCommands{
        @Override
        public String getDescription() {
            return "Авторы проекта";
        }

        @Override
        public String getContent() {
            return "Бота создали Бабенко Андрей (@minofprop) и Кухтей Дмитрий (@sculp2ra). Спасибо за интерес!";
        }

        @Override
        public String getCommand() {
            return "/authors";
        }

        public InlineKeyboardMarkup getKeyboard(){
            return Keyboards.getToMenuKeyboard();
    }
}
