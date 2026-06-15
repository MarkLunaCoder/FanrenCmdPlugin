package com.mark.fanreninput;

final class CommandItem {
    final String category;
    final String title;
    final String command;
    final String description;

    CommandItem(String category, String title, String command, String description) {
        this.category = category;
        this.title = title;
        this.command = command;
        this.description = description;
    }
}
