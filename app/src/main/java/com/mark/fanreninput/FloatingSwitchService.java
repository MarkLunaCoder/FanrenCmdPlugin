package com.mark.fanreninput;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FloatingSwitchService extends Service {
    private static final int MIN_COMMIT_INTERVAL_MS = 700;
    private static final int PANEL_MARGIN_DP = 12;
    private static final int FLOATING_BUTTON_SIZE_DP = 68;

    private WindowManager windowManager;
    private View floatingView;
    private View commandPanel;
    private WindowManager.LayoutParams layoutParams;
    private WindowManager.LayoutParams panelLayoutParams;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private List<CommandItem> allCommands = new ArrayList<>();
    private CommandCatalog commandCatalog = CommandCatalog.from(new ArrayList<>());
    private String selectedCategory = CommandCatalog.FAVORITE_CATEGORY;
    private LinearLayout panelCategoryContainer;
    private LinearLayout panelCommandContainer;
    private TextView panelStatusText;
    private EditText panelSearchInput;
    private View panelCategoryScroll;
    private View panelDivider;
    private final List<TextView> panelCategoryTabs = new ArrayList<>();
    private List<CommandItem> favoriteCommandSnapshot = new ArrayList<>();
    private Set<String> favoriteCommandSet = new HashSet<>();
    private String currentSearchQuery = "";
    private int lastRenderedCommandCount;
    private long lastCommitAt;

    public static void start(Context context) {
        Intent intent = new Intent(context, FloatingSwitchService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, FloatingSwitchService.class);
        context.stopService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!canDrawOverlays()) {
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        allCommands = CommandRepository.load(this);
        commandCatalog = CommandCatalog.from(allCommands);
        floatingView = View.inflate(this, R.layout.floating_switch_view, null);
        layoutParams = createLayoutParams();
        bindTouchEvents();
        windowManager.addView(floatingView, layoutParams);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideCommandPanel();
        if (windowManager != null && floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    @SuppressWarnings("deprecation")
    private WindowManager.LayoutParams createLayoutParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        params.x = screenWidth - dp(FLOATING_BUTTON_SIZE_DP + 36);
        params.y = clamp(screenHeight - dp(280), dp(160), screenHeight - dp(128));
        return params;
    }

    private void bindTouchEvents() {
        View.OnTouchListener listener = (view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = layoutParams.x;
                    initialY = layoutParams.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    layoutParams.x = initialX + Math.round(event.getRawX() - initialTouchX);
                    layoutParams.y = initialY + Math.round(event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, layoutParams);
                    updateCommandPanelPosition();
                    return true;
                case MotionEvent.ACTION_UP:
                    float dx = Math.abs(event.getRawX() - initialTouchX);
                    float dy = Math.abs(event.getRawY() - initialTouchY);
                    if (dx < dp(8) && dy < dp(8)) {
                        handleSwitchClick();
                    }
                    return true;
                default:
                    return false;
            }
        };
        floatingView.setOnTouchListener(listener);
        View button = floatingView.findViewById(R.id.floatingSwitchButton);
        View hint = floatingView.findViewById(R.id.floatingSwitchHint);
        if (button != null) {
            button.setOnTouchListener(listener);
        }
        if (hint != null) {
            hint.setOnTouchListener(listener);
        }
    }

    private void handleSwitchClick() {
        toggleCommandPanel();
    }

    private void toggleCommandPanel() {
        if (commandPanel != null) {
            hideCommandPanel();
            return;
        }
        showCommandPanel();
    }

    private void showCommandPanel() {
        if (windowManager == null || commandPanel != null) {
            return;
        }
        commandPanel = createCommandPanel();
        panelLayoutParams = createPanelLayoutParams();
        buildPanelCategories();
        renderPanelCommands();
        updatePanelStatus();
        windowManager.addView(commandPanel, panelLayoutParams);
    }

    private void hideCommandPanel() {
        if (windowManager != null && commandPanel != null) {
            windowManager.removeView(commandPanel);
        }
        commandPanel = null;
        panelCategoryContainer = null;
        panelCommandContainer = null;
        panelStatusText = null;
        panelSearchInput = null;
        panelCategoryScroll = null;
        panelDivider = null;
        panelCategoryTabs.clear();
        currentSearchQuery = "";
        lastRenderedCommandCount = 0;
    }

    @SuppressWarnings("deprecation")
    private WindowManager.LayoutParams createPanelLayoutParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int margin = dp(PANEL_MARGIN_DP);
        int panelWidth = clamp(
                Math.round(screenWidth * 0.74f),
                dp(282),
                Math.min(screenWidth - margin * 2, dp(340))
        );
        int panelHeight = clamp(
                Math.round(screenHeight * 0.46f),
                dp(350),
                Math.min(screenHeight - margin * 2, dp(430))
        );
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                panelWidth,
                panelHeight,
                type,
                PanelWindowFocusPolicy.flagsForInputFocus(false),
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        positionPanelLayoutParams(params, screenWidth, screenHeight);
        return params;
    }

    private void updateCommandPanelPosition() {
        if (windowManager == null || commandPanel == null || panelLayoutParams == null) {
            return;
        }
        positionPanelLayoutParams(
                panelLayoutParams,
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels
        );
        windowManager.updateViewLayout(commandPanel, panelLayoutParams);
    }

    private void positionPanelLayoutParams(
            WindowManager.LayoutParams params,
            int screenWidth,
            int screenHeight
    ) {
        int margin = dp(PANEL_MARGIN_DP);
        int buttonCenterX = layoutParams.x + dp(FLOATING_BUTTON_SIZE_DP / 2);
        int buttonCenterY = layoutParams.y + dp(FLOATING_BUTTON_SIZE_DP / 2);
        int desiredX = buttonCenterX - params.width + dp(18);
        int desiredY = buttonCenterY - params.height - dp(12);
        if (desiredY < margin) {
            desiredY = buttonCenterY + dp(42);
        }
        params.x = clamp(desiredX, margin, screenWidth - params.width - margin);
        params.y = clamp(desiredY, margin, screenHeight - params.height - margin);
    }

    private View createCommandPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.floating_command_panel_bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            root.setElevation(dp(14));
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), 0, dp(10), 0);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("指令面板");
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextColor(0xFF1D7D63);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        titleGroup.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView subtitle = new TextView(this);
        subtitle.setText("凡人修仙");
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        subtitle.setTextColor(0xFF69766F);
        subtitle.setTextSize(10);
        subtitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(3), 0, 0);
        titleGroup.addView(subtitle, subtitleParams);

        header.addView(titleGroup, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));

        TextView closeButton = new TextView(this);
        closeButton.setText("×");
        closeButton.setTextSize(16);
        closeButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setTextColor(0xFF4D5652);
        closeButton.setBackgroundResource(R.drawable.floating_panel_close_bg);
        closeButton.setOnClickListener(view -> hideCommandPanel());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        closeParams.setMargins(dp(8), 0, 0, 0);
        header.addView(closeButton, closeParams);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
        ));

        panelSearchInput = new EditText(this);
        panelSearchInput.setSingleLine(true);
        panelSearchInput.setHint("搜索指令或说明");
        panelSearchInput.setHintTextColor(0xFF8E948D);
        panelSearchInput.setTextColor(0xFF1F2423);
        panelSearchInput.setTextSize(13);
        panelSearchInput.setIncludeFontPadding(false);
        panelSearchInput.setPadding(dp(12), 0, dp(12), 0);
        panelSearchInput.setBackgroundResource(R.drawable.floating_panel_search_bg);
        panelSearchInput.setOnTouchListener((view, event) -> {
            enablePanelInputFocus();
            return false;
        });
        panelSearchInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                disablePanelInputFocus();
            }
        });
        panelSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                currentSearchQuery = text == null ? "" : text.toString();
                updatePanelCategoryVisibility();
                renderPanelCommands();
                updatePanelStatus();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
        );
        searchParams.setMargins(dp(12), 0, dp(12), dp(2));
        root.addView(panelSearchInput, searchParams);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(dp(10), dp(10), dp(10), dp(8));

        ScrollView categoryScroll = new ScrollView(this);
        panelCategoryScroll = categoryScroll;
        categoryScroll.setFillViewport(false);
        categoryScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        categoryScroll.setVerticalScrollBarEnabled(false);
        panelCategoryContainer = new LinearLayout(this);
        panelCategoryContainer.setOrientation(LinearLayout.VERTICAL);
        categoryScroll.addView(panelCategoryContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        body.addView(categoryScroll, new LinearLayout.LayoutParams(
                dp(76),
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        View divider = new View(this);
        panelDivider = divider;
        divider.setBackgroundColor(0xFFE6DECF);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                dp(1),
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        dividerParams.setMargins(dp(8), 0, dp(8), 0);
        body.addView(divider, dividerParams);

        ScrollView commandScroll = new ScrollView(this);
        commandScroll.setFillViewport(false);
        commandScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        commandScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        panelCommandContainer = new LinearLayout(this);
        panelCommandContainer.setOrientation(LinearLayout.VERTICAL);
        commandScroll.addView(panelCommandContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        LinearLayout.LayoutParams commandParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        );
        body.addView(commandScroll, commandParams);

        root.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        panelStatusText = new TextView(this);
        panelStatusText.setSingleLine(true);
        panelStatusText.setEllipsize(TextUtils.TruncateAt.END);
        panelStatusText.setGravity(Gravity.CENTER_VERTICAL);
        panelStatusText.setPadding(dp(12), 0, dp(12), 0);
        panelStatusText.setTextColor(0xFF6F756F);
        panelStatusText.setTextSize(11);
        root.addView(panelStatusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28)
        ));
        return root;
    }

    private void updatePanelStatus() {
        if (panelStatusText == null) {
            return;
        }
        String status = AccessibilityServiceState.isEnabled(this)
                ? "点击后填入当前输入框"
                : "未启用无障碍，点击后复制";
        if (isSearching()) {
            status = lastRenderedCommandCount > 0
                    ? "找到 " + lastRenderedCommandCount + " 条相关指令"
                    : "未找到相关指令";
        }
        panelStatusText.setText(status);
    }

    private void buildPanelCategories() {
        if (panelCategoryContainer == null) {
            return;
        }
        updatePanelCategoryVisibility();
        panelCategoryContainer.removeAllViews();
        panelCategoryTabs.clear();
        for (String category : commandCatalog.categories()) {
            TextView tab = new TextView(this);
            tab.setText(category);
            tab.setTextSize(12);
            tab.setGravity(Gravity.CENTER);
            tab.setSingleLine(false);
            tab.setMaxLines(2);
            tab.setEllipsize(TextUtils.TruncateAt.END);
            tab.setIncludeFontPadding(false);
            tab.setPadding(dp(4), dp(1), dp(4), dp(1));
            tab.setTag(category);
            tab.setOnClickListener(view -> {
                selectedCategory = category;
                updatePanelCategorySelection();
                renderPanelCommands();
                updatePanelStatus();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(32)
            );
            params.setMargins(0, 0, 0, dp(8));
            panelCategoryContainer.addView(tab, params);
            panelCategoryTabs.add(tab);
        }
        updatePanelCategorySelection();
    }

    private void updatePanelCategoryVisibility() {
        boolean searching = isSearching();
        if (panelCategoryScroll != null) {
            panelCategoryScroll.setVisibility(searching ? View.GONE : View.VISIBLE);
        }
        if (panelDivider != null) {
            panelDivider.setVisibility(searching ? View.GONE : View.VISIBLE);
        }
    }

    private void updatePanelCategorySelection() {
        for (TextView tab : panelCategoryTabs) {
            Object tag = tab.getTag();
            boolean selected = selectedCategory.equals(tag);
            tab.setBackgroundResource(selected
                    ? R.drawable.floating_panel_category_selected_bg
                    : R.drawable.floating_panel_category_bg);
            tab.setTextColor(selected ? 0xFFFFFFFF : 0xFF52615D);
            tab.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void renderPanelCommands() {
        if (panelCommandContainer == null) {
            return;
        }
        panelCommandContainer.removeAllViews();
        refreshFavoriteCommandSet();
        boolean searching = isSearching();
        List<CommandItem> commands = commandsForCurrentPanel();
        lastRenderedCommandCount = commands.size();
        if (commands.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(searching ? "未找到相关指令" : "暂无记录");
            emptyView.setTextColor(0xFF5F5B52);
            emptyView.setTextSize(13);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setMinHeight(dp(80));
            panelCommandContainer.addView(emptyView);
            return;
        }

        for (CommandItem item : commands) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(58));
            row.setPadding(dp(4), dp(6), dp(2), dp(6));
            row.setBackgroundResource(R.drawable.floating_panel_command_row_bg);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(view -> commitCommandFromPanel(item));
            row.setOnLongClickListener(view -> {
                showPanelCommandMenu(view, item);
                return true;
            });

            LinearLayout textGroup = new LinearLayout(this);
            textGroup.setOrientation(LinearLayout.VERTICAL);
            textGroup.setGravity(Gravity.CENTER_VERTICAL);

            TextView commandTitleText = new TextView(this);
            commandTitleText.setText(CommandTextFormatter.buildCommandTitleText(item.command));
            commandTitleText.setTextSize(13);
            commandTitleText.setSingleLine(true);
            commandTitleText.setEllipsize(TextUtils.TruncateAt.END);
            commandTitleText.setIncludeFontPadding(false);
            textGroup.addView(commandTitleText, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            if (!item.description.isEmpty()) {
                TextView descriptionText = new TextView(this);
                descriptionText.setText(item.description);
                descriptionText.setTextSize(12);
                descriptionText.setTextColor(0xFF5F6863);
                descriptionText.setLineSpacing(dp(1), 1.0f);
                descriptionText.setMaxLines(2);
                descriptionText.setEllipsize(TextUtils.TruncateAt.END);
                descriptionText.setIncludeFontPadding(false);
                LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                descriptionParams.setMargins(0, dp(4), 0, 0);
                textGroup.addView(descriptionText, descriptionParams);
            }

            row.addView(textGroup, new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            ));

            if (favoriteCommandSet.contains(item.command)) {
                TextView star = new TextView(this);
                star.setText("★");
                star.setTextSize(14);
                star.setTextColor(0xFFB8792D);
                star.setGravity(Gravity.CENTER);
                row.addView(star, new LinearLayout.LayoutParams(dp(22), dp(44)));
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dp(2));
            panelCommandContainer.addView(row, params);
        }
    }

    private List<CommandItem> commandsForCurrentPanel() {
        if (isSearching()) {
            return CommandSearch.filter(commandCatalog.allCommands(), currentSearchQuery);
        }
        if (CommandCatalog.FAVORITE_CATEGORY.equals(selectedCategory)
                || CommandCatalog.RECENT_CATEGORY.equals(selectedCategory)) {
            if (CommandCatalog.FAVORITE_CATEGORY.equals(selectedCategory)) {
                return favoriteCommandSnapshot;
            }
            return CommandStateStore.commandsForCategory(this, allCommands, selectedCategory);
        }
        return commandCatalog.commandsForCategory(selectedCategory);
    }

    private void refreshFavoriteCommandSet() {
        List<CommandItem> favoriteCommands = CommandStateStore.favoriteCommandItems(this, allCommands);
        Set<String> commands = new HashSet<>();
        for (CommandItem item : favoriteCommands) {
            commands.add(item.command);
        }
        favoriteCommandSnapshot = favoriteCommands;
        favoriteCommandSet = commands;
    }

    private void commitCommandFromPanel(CommandItem item) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastCommitAt < MIN_COMMIT_INTERVAL_MS) {
            Toast.makeText(this, "操作太快，稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        lastCommitAt = now;
        String command = CommandTextFormatter.cleanCommandForCommit(item.command);
        if (panelSearchInput != null) {
            panelSearchInput.clearFocus();
        }
        disablePanelInputFocus();
        hideCommandPanel();
        CommandStateStore.saveRecentCommand(this, item.command);

        if (!AccessibilityServiceState.isEnabled(this)) {
            copyToClipboard(command);
            Toast.makeText(this, "未启用无障碍，指令已复制", Toast.LENGTH_SHORT).show();
            return;
        }

        FanRenAccessibilityService.CommitResult result =
                FanRenAccessibilityService.commitCommandToActiveInput(command);
        switch (result) {
            case SUCCESS:
                Toast.makeText(this, "已填入指令", Toast.LENGTH_SHORT).show();
                break;
            case INPUT_NOT_FOUND:
                copyToClipboard(command);
                Toast.makeText(this, "未找到当前应用输入框，指令已复制", Toast.LENGTH_SHORT).show();
                break;
            case SERVICE_UNAVAILABLE:
            case ACTION_FAILED:
            default:
                copyToClipboard(command);
                Toast.makeText(this, "自动填入失败，指令已复制", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showPanelCommandMenu(View anchor, CommandItem item) {
        List<MenuAction> actions = menuActionsFor(item);
        if (actions.isEmpty()) {
            return;
        }
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackgroundResource(R.drawable.command_menu_bg);
        menu.setPadding(0, dp(4), 0, dp(4));
        int popupWidth = Math.max(dp(136), anchor.getWidth() / 2);
        PopupWindow popup = new PopupWindow(menu,
                popupWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable(0x00000000));
        for (MenuAction action : actions) {
            TextView itemView = new TextView(this);
            itemView.setText(action.label);
            itemView.setTextSize(15);
            itemView.setTextColor(0xFF222222);
            itemView.setGravity(Gravity.CENTER_VERTICAL);
            itemView.setPadding(dp(14), 0, dp(14), 0);
            itemView.setMinHeight(dp(42));
            itemView.setBackgroundResource(R.drawable.command_menu_item_bg);
            itemView.setOnClickListener(view -> {
                popup.dismiss();
                action.run();
            });
            menu.addView(itemView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(42)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popup.setElevation(dp(6));
        }
        popup.showAsDropDown(anchor, 0, -anchor.getHeight());
    }

    private List<MenuAction> menuActionsFor(CommandItem item) {
        refreshFavoriteCommandSet();
        List<MenuAction> actions = new ArrayList<>();
        if (isSearching()) {
            if (favoriteCommandSet.contains(item.command)) {
                actions.add(new MenuAction("从常用移除", () -> removeFavoriteCommand(item)));
            } else {
                actions.add(new MenuAction("加入常用", () -> addFavoriteCommand(item)));
            }
            return actions;
        }
        if (CommandCatalog.FAVORITE_CATEGORY.equals(selectedCategory)) {
            actions.add(new MenuAction("上移", () -> moveFavoriteCommand(item, CommandStateStore.FavoriteMove.UP)));
            actions.add(new MenuAction("下移", () -> moveFavoriteCommand(item, CommandStateStore.FavoriteMove.DOWN)));
            actions.add(new MenuAction("移至顶部", () -> moveFavoriteCommand(item, CommandStateStore.FavoriteMove.TOP)));
            actions.add(new MenuAction("移至底部", () -> moveFavoriteCommand(item, CommandStateStore.FavoriteMove.BOTTOM)));
            actions.add(new MenuAction("从常用移除", () -> removeFavoriteCommand(item)));
            return actions;
        }
        if (favoriteCommandSet.contains(item.command)) {
            actions.add(new MenuAction("从常用移除", () -> removeFavoriteCommand(item)));
        } else {
            actions.add(new MenuAction("加入常用", () -> addFavoriteCommand(item)));
        }
        return actions;
    }

    private void addFavoriteCommand(CommandItem item) {
        if (!CommandStateStore.addFavoriteCommand(this, allCommands, item)) {
            Toast.makeText(this, "已在常用中", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "已加入常用", Toast.LENGTH_SHORT).show();
        renderPanelCommands();
        updatePanelStatus();
    }

    private void removeFavoriteCommand(CommandItem item) {
        if (!CommandStateStore.removeFavoriteCommand(this, allCommands, item)) {
            return;
        }
        Toast.makeText(this, "已从常用移除", Toast.LENGTH_SHORT).show();
        renderPanelCommands();
        updatePanelStatus();
    }

    private void moveFavoriteCommand(CommandItem item, CommandStateStore.FavoriteMove move) {
        if (CommandStateStore.moveFavoriteCommand(this, allCommands, item, move)) {
            renderPanelCommands();
            updatePanelStatus();
        }
    }

    private void copyToClipboard(String command) {
        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("fanren_command", command));
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean isSearching() {
        return !TextUtils.isEmpty(currentSearchQuery == null ? "" : currentSearchQuery.trim());
    }

    private void enablePanelInputFocus() {
        updatePanelInputFocus(true);
    }

    private void disablePanelInputFocus() {
        updatePanelInputFocus(false);
    }

    private void updatePanelInputFocus(boolean allowInputFocus) {
        if (windowManager == null || commandPanel == null || panelLayoutParams == null) {
            return;
        }
        int flags = PanelWindowFocusPolicy.flagsForInputFocus(allowInputFocus);
        if (panelLayoutParams.flags == flags) {
            return;
        }
        panelLayoutParams.flags = flags;
        panelLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        windowManager.updateViewLayout(commandPanel, panelLayoutParams);
    }

    private static final class MenuAction {
        final String label;
        final Runnable runnable;

        MenuAction(String label, Runnable runnable) {
            this.label = label;
            this.runnable = runnable;
        }

        void run() {
            runnable.run();
        }
    }
}
