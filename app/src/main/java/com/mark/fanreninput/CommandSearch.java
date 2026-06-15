package com.mark.fanreninput;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CommandSearch {
    private CommandSearch() {
    }

    static List<CommandItem> filter(List<CommandItem> commands, String query) {
        String keyword = normalize(query);
        List<CommandItem> result = new ArrayList<>();
        if (keyword.isEmpty()) {
            return result;
        }
        for (CommandItem item : commands) {
            if (matches(item, keyword)) {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean matches(CommandItem item, String keyword) {
        return contains(item.category, keyword)
                || contains(item.title, keyword)
                || contains(item.command, keyword)
                || contains(item.description, keyword);
    }

    private static boolean contains(String text, String keyword) {
        return normalize(text).contains(keyword);
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }
}
