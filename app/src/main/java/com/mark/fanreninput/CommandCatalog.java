package com.mark.fanreninput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CommandCatalog {
    static final String FAVORITE_CATEGORY = "常用";
    static final String RECENT_CATEGORY = "最近";

    private final List<CommandItem> allCommands;
    private final List<String> categories;
    private final Map<String, List<CommandItem>> commandsByCategory;

    private CommandCatalog(
            List<CommandItem> allCommands,
            List<String> categories,
            Map<String, List<CommandItem>> commandsByCategory
    ) {
        this.allCommands = allCommands;
        this.categories = categories;
        this.commandsByCategory = commandsByCategory;
    }

    static CommandCatalog from(List<CommandItem> commands) {
        List<CommandItem> indexedCommands = Collections.unmodifiableList(new ArrayList<>(commands));
        Map<String, List<CommandItem>> mutableCommandsByCategory = new LinkedHashMap<>();
        for (CommandItem command : commands) {
            mutableCommandsByCategory
                    .computeIfAbsent(command.category, ignored -> new ArrayList<>())
                    .add(command);
        }

        Map<String, List<CommandItem>> indexedCommandsByCategory = new LinkedHashMap<>();
        for (Map.Entry<String, List<CommandItem>> entry : mutableCommandsByCategory.entrySet()) {
            indexedCommandsByCategory.put(
                    entry.getKey(),
                    Collections.unmodifiableList(new ArrayList<>(entry.getValue()))
            );
        }

        List<String> indexedCategories = new ArrayList<>();
        indexedCategories.add(FAVORITE_CATEGORY);
        indexedCategories.add(RECENT_CATEGORY);
        indexedCategories.addAll(indexedCommandsByCategory.keySet());

        return new CommandCatalog(
                indexedCommands,
                Collections.unmodifiableList(indexedCategories),
                Collections.unmodifiableMap(indexedCommandsByCategory)
        );
    }

    List<CommandItem> allCommands() {
        return allCommands;
    }

    List<String> categories() {
        return categories;
    }

    List<CommandItem> commandsForCategory(String category) {
        List<CommandItem> commands = commandsByCategory.get(category);
        if (commands == null) {
            return Collections.emptyList();
        }
        return commands;
    }
}
