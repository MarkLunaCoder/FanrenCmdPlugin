package com.mark.fanreninput;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandSearchTest {
    @Test
    public void filterReturnsMatchesFromCommandTitleDescriptionAndCategory() {
        List<CommandItem> commands = sampleCommands();

        assertEquals(Collections.singletonList(commands.get(0)), CommandSearch.filter(commands, "灵根"));
        assertEquals(Collections.singletonList(commands.get(1)), CommandSearch.filter(commands, ".宗门"));
        assertEquals(Collections.singletonList(commands.get(1)), CommandSearch.filter(commands, "签到"));
        assertEquals(Collections.singletonList(commands.get(2)), CommandSearch.filter(commands, "坊市"));
    }

    @Test
    public void filterIgnoresCaseAndTrimsKeyword() {
        List<CommandItem> commands = sampleCommands();

        assertEquals(Collections.singletonList(commands.get(2)), CommandSearch.filter(commands, " buy "));
    }

    @Test
    public void filterReturnsEmptyListForBlankKeyword() {
        assertTrue(CommandSearch.filter(sampleCommands(), "   ").isEmpty());
    }

    @Test
    public void filterReturnsEmptyListWhenKeywordDoesNotMatch() {
        assertTrue(CommandSearch.filter(sampleCommands(), "不存在").isEmpty());
    }

    private List<CommandItem> sampleCommands() {
        return Arrays.asList(
                new CommandItem("入门与角色", "检测灵根", ".检测灵根", "开始修仙。"),
                new CommandItem("宗门", "宗门点卯", ".宗门点卯", "每日签到。"),
                new CommandItem("坊市", "坊市购买", ".buy <item>", "购买道具。")
        );
    }
}
