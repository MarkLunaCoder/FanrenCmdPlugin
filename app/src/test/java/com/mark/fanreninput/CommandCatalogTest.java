package com.mark.fanreninput;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CommandCatalogTest {
    @Test
    public void categoriesStartWithDynamicCategoriesThenUniqueCommandCategories() {
        CommandCatalog catalog = CommandCatalog.from(sampleCommands());

        assertEquals(
                Arrays.asList("常用", "最近", "入门与角色", "宗门", "坊市"),
                catalog.categories()
        );
    }

    @Test
    public void commandsForCategoryReturnsIndexedCommandsInOriginalOrder() {
        List<CommandItem> commands = sampleCommands();
        CommandCatalog catalog = CommandCatalog.from(commands);

        assertEquals(
                Arrays.asList(commands.get(1), commands.get(3)),
                catalog.commandsForCategory("宗门")
        );
        assertEquals(Collections.emptyList(), catalog.commandsForCategory("不存在"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void categoriesCannotBeModifiedByCallers() {
        CommandCatalog catalog = CommandCatalog.from(sampleCommands());

        catalog.categories().add("炼丹");
    }

    private List<CommandItem> sampleCommands() {
        return Arrays.asList(
                new CommandItem("入门与角色", "检测灵根", ".检测灵根", "开始修仙。"),
                new CommandItem("宗门", "宗门点卯", ".宗门点卯", "每日签到。"),
                new CommandItem("坊市", "坊市购买", ".buy <item>", "购买道具。"),
                new CommandItem("宗门", "宗门任务", ".宗门任务", "领取宗门任务。")
        );
    }
}
