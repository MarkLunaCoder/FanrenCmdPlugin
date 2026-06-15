package com.mark.fanreninput;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CommandTextFormatter {
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(<[^>]+>|\\[[^\\]]+])");

    private CommandTextFormatter() {
    }

    static String cleanCommandForCommit(String command) {
        String cleaned = command
                .replaceAll("\\s*<[^>]+>", "")
                .replaceAll("\\s*\\[[^\\]]+]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return command;
        }
        boolean removedParameter = !cleaned.equals(command.trim());
        return removedParameter ? cleaned + " " : cleaned;
    }

    static SpannableString buildCommandText(CommandItem item) {
        String text = item.command + "\n" + item.description;
        SpannableString spannable = new SpannableString(text);
        int commandEnd = item.command.length();
        applyCommandSpans(spannable, item.command, 0, commandEnd);

        if (!item.description.isEmpty()) {
            int descriptionStart = commandEnd + 1;
            spannable.setSpan(new ForegroundColorSpan(0xFF5F5B52), descriptionStart, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    static SpannableString buildCommandTitleText(String command) {
        SpannableString spannable = new SpannableString(command);
        applyCommandSpans(spannable, command, 0, command.length());
        return spannable;
    }

    private static void applyCommandSpans(
            SpannableString spannable,
            String command,
            int start,
            int end
    ) {
        spannable.setSpan(new ForegroundColorSpan(0xFF246B57), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        Matcher matcher = PARAMETER_PATTERN.matcher(command);
        while (matcher.find()) {
            int parameterStart = start + matcher.start();
            int parameterEnd = start + matcher.end();
            spannable.setSpan(new ForegroundColorSpan(0xFF9A6A22), parameterStart, parameterEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.NORMAL), parameterStart, parameterEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
