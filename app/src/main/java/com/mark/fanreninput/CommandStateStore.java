package com.mark.fanreninput;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

final class CommandStateStore {
    private static final String PREFS_NAME = "fanren_input";
    private static final String RECENT_COMMANDS = "recent_commands";
    private static final String FAVORITE_COMMANDS = "favorite_commands";
    private static final int MAX_RECENT = 12;
    private static final List<String> DEFAULT_COMMON_COMMANDS = Arrays.asList(
            ".帮助",
            ".检测灵根",
            ".我的灵根",
            ".状态",
            ".储物袋",
            ".闭关修炼",
            ".深度闭关",
            ".查看闭关",
            ".宗门点卯",
            ".宗门传功",
            ".我的宗门",
            ".小药园"
    );

    private CommandStateStore() {
    }

    static List<CommandItem> commandsForCategory(
            Context context,
            List<CommandItem> allCommands,
            String selectedCategory
    ) {
        if ("常用".equals(selectedCategory)) {
            return favoriteCommandItems(context, allCommands);
        }
        if ("最近".equals(selectedCategory)) {
            return recentCommandItems(context, allCommands);
        }
        List<CommandItem> commands = new ArrayList<>();
        for (CommandItem item : allCommands) {
            if (selectedCategory.equals(item.category)) {
                commands.add(item);
            }
        }
        return commands;
    }

    static List<CommandItem> favoriteCommandItems(Context context, List<CommandItem> allCommands) {
        List<CommandItem> commands = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String command : readFavoriteCommands(context, allCommands)) {
            CommandItem item = findCommandItem(allCommands, command);
            if (item != null && seen.add(item.command)) {
                commands.add(item);
            }
        }
        return commands;
    }

    static boolean isFavorite(Context context, List<CommandItem> allCommands, CommandItem item) {
        return readFavoriteCommands(context, allCommands).contains(item.command);
    }

    static boolean addFavoriteCommand(Context context, List<CommandItem> allCommands, CommandItem item) {
        List<String> commands = readFavoriteCommands(context, allCommands);
        if (commands.contains(item.command)) {
            return false;
        }
        commands.add(item.command);
        saveFavoriteCommands(context, commands);
        return true;
    }

    static boolean removeFavoriteCommand(Context context, List<CommandItem> allCommands, CommandItem item) {
        List<String> commands = readFavoriteCommands(context, allCommands);
        if (!commands.remove(item.command)) {
            return false;
        }
        saveFavoriteCommands(context, commands);
        return true;
    }

    static boolean moveFavoriteCommand(
            Context context,
            List<CommandItem> allCommands,
            CommandItem item,
            FavoriteMove move
    ) {
        List<String> commands = readFavoriteCommands(context, allCommands);
        int index = commands.indexOf(item.command);
        if (index < 0) {
            return false;
        }
        int targetIndex = index;
        switch (move) {
            case UP:
                targetIndex = Math.max(0, index - 1);
                break;
            case DOWN:
                targetIndex = Math.min(commands.size() - 1, index + 1);
                break;
            case TOP:
                targetIndex = 0;
                break;
            case BOTTOM:
                targetIndex = commands.size() - 1;
                break;
        }
        if (targetIndex == index) {
            return false;
        }
        commands.remove(index);
        commands.add(targetIndex, item.command);
        saveFavoriteCommands(context, commands);
        return true;
    }

    static void saveRecentCommand(Context context, String command) {
        LinkedHashSet<String> commands = new LinkedHashSet<>();
        commands.add(command);
        for (String item : readRecentCommands(context)) {
            commands.add(item);
            if (commands.size() >= MAX_RECENT) {
                break;
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(RECENT_COMMANDS, joinCommandList(commands))
                .apply();
    }

    static CommandItem findCommandItem(List<CommandItem> allCommands, String command) {
        for (CommandItem item : allCommands) {
            if (item.command.equals(command)) {
                return item;
            }
        }
        return null;
    }

    private static List<String> readFavoriteCommands(Context context, List<CommandItem> allCommands) {
        String value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(FAVORITE_COMMANDS, null);
        if (value == null) {
            return defaultFavoriteCommands(allCommands);
        }
        return parseStoredCommands(value, allCommands);
    }

    private static List<String> defaultFavoriteCommands(List<CommandItem> allCommands) {
        List<String> result = new ArrayList<>();
        for (String command : DEFAULT_COMMON_COMMANDS) {
            if (findCommandItem(allCommands, command) != null) {
                result.add(command);
            }
        }
        return result;
    }

    private static List<String> parseStoredCommands(String value, List<CommandItem> allCommands) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return result;
        }
        LinkedHashSet<String> uniqueCommands = new LinkedHashSet<>();
        for (String item : value.split("\\n")) {
            String command = item.trim();
            if (!command.isEmpty() && findCommandItem(allCommands, command) != null) {
                uniqueCommands.add(command);
            }
        }
        result.addAll(uniqueCommands);
        return result;
    }

    private static void saveFavoriteCommands(Context context, List<String> commands) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(FAVORITE_COMMANDS, joinCommandList(commands))
                .apply();
    }

    private static List<CommandItem> recentCommandItems(Context context, List<CommandItem> allCommands) {
        List<String> recentCommands = readRecentCommands(context);
        List<CommandItem> result = new ArrayList<>();
        for (String commandText : recentCommands) {
            CommandItem item = findCommandItem(allCommands, commandText);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private static List<String> readRecentCommands(Context context) {
        String value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(RECENT_COMMANDS, "");
        List<String> result = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return result;
        }
        for (String item : value.split("\\n")) {
            String command = item.trim();
            if (!command.isEmpty()) {
                result.add(command);
            }
        }
        return result;
    }

    private static String joinCommandList(Iterable<String> commands) {
        StringBuilder builder = new StringBuilder();
        for (String command : commands) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(command);
        }
        return builder.toString();
    }

    enum FavoriteMove {
        UP,
        DOWN,
        TOP,
        BOTTOM
    }
}
