package ru.outfix.market.util;

/** Подготовка пользовательского текста для сообщений с {@code parse_mode=HTML}. */
public final class Html {

    private Html() {
    }

    public static String escape(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
