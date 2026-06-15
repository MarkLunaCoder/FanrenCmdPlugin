package com.mark.fanreninput;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class CommandRepository {
    private static final String COMMAND_ASSET = "xiuxian_game_commands.md";

    private CommandRepository() {
    }

    static List<CommandItem> load(Context context) {
        try (InputStream inputStream = context.getAssets().open(COMMAND_ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<CommandItem> parsed = parse(reader);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        } catch (IOException ignored) {
            // 首次安装或 assets 异常时，保留最小可用命令。
        }
        return fallbackCommands();
    }

    static List<CommandItem> parse(BufferedReader reader) throws IOException {
        List<CommandItem> commands = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String category = "未分类";
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## ")) {
                category = trimmed.replaceFirst("^#+\\s*", "").trim();
                continue;
            }
            if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) {
                continue;
            }
            List<String> cells = splitTableRow(trimmed);
            if (cells.size() < 2) {
                continue;
            }
            String commandCell = cells.get(0).trim();
            String description = cleanMarkdown(cells.get(1).trim());
            if (commandCell.equals("命令") || commandCell.contains("---")) {
                continue;
            }
            for (String rawCommand : splitCommandVariants(commandCell)) {
                String command = cleanMarkdown(rawCommand);
                if (!command.startsWith(".")) {
                    continue;
                }
                String key = category + "|" + command;
                if (!seen.add(key)) {
                    continue;
                }
                commands.add(new CommandItem(category, extractTitle(command), command, description));
            }
        }
        return commands;
    }

    private static List<String> splitTableRow(String row) {
        String body = row.substring(1, row.length() - 1);
        String[] parts = body.split("\\|");
        List<String> cells = new ArrayList<>(parts.length);
        for (String part : parts) {
            cells.add(part.trim());
        }
        return cells;
    }

    private static List<String> splitCommandVariants(String commandCell) {
        String[] parts = commandCell.split("\\s+/\\s+");
        List<String> variants = new ArrayList<>(parts.length);
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                variants.add(value);
            }
        }
        return variants;
    }

    private static String cleanMarkdown(String value) {
        return value
                .replace("`", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .trim();
    }

    private static String extractTitle(String command) {
        String title = command.startsWith(".") ? command.substring(1) : command;
        int spaceIndex = title.indexOf(' ');
        if (spaceIndex >= 0) {
            title = title.substring(0, spaceIndex);
        }
        return title.trim();
    }

    private static List<CommandItem> fallbackCommands() {
        List<CommandItem> commands = new ArrayList<>();
        commands.add(new CommandItem("入门与角色", "帮助", ".帮助", "查看基础帮助。"));
        commands.add(new CommandItem("入门与角色", "检测灵根", ".检测灵根", "开始修仙/检测灵根。"));
        commands.add(new CommandItem("入门与角色", "我的灵根", ".我的灵根", "查看自己的修士面板。"));
        commands.add(new CommandItem("修炼与突破", "闭关修炼", ".闭关修炼", "常规修炼。"));
        commands.add(new CommandItem("宗门", "宗门点卯", ".宗门点卯", "每日宗门签到。"));
        return commands;
    }
}
