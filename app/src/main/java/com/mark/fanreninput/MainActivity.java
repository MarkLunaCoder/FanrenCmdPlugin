package com.mark.fanreninput;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "fanren_input";
    private static final String FLOATING_ENABLED = "floating_enabled";

    private TextView statusText;
    private Button floatingButtonToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        Button openTelegramButton = findViewById(R.id.openTelegramButton);
        Button overlayPermissionButton = findViewById(R.id.overlayPermissionButton);
        Button accessibilityPermissionButton = findViewById(R.id.accessibilityPermissionButton);
        floatingButtonToggle = findViewById(R.id.floatingButtonToggle);

        openTelegramButton.setOnClickListener(view -> openTelegram());

        overlayPermissionButton.setOnClickListener(view -> openOverlaySettings());
        accessibilityPermissionButton.setOnClickListener(view -> openAccessibilitySettings());
        floatingButtonToggle.setOnClickListener(view -> toggleFloatingButton());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        syncFloatingButtonState();
    }

    private void updateStatus() {
        boolean overlayAllowed = canDrawOverlays();
        boolean accessibilityEnabled = AccessibilityServiceState.isEnabled(this);
        String status = overlayAllowed ? "悬浮按钮：已授权。" : "悬浮按钮：未授权。";
        status += accessibilityEnabled ? "\n无障碍自动填入：已授权。" : "\n无障碍自动填入：未授权。";
        status += "\n当前推荐：开启悬浮按钮，并授权无障碍自动填入后跨应用填入指令。";
        statusText.setText(status);
    }

    private void openTelegram() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("org.telegram.messenger");
        if (launchIntent != null) {
            startActivity(launchIntent);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve")));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "未找到 Telegram", Toast.LENGTH_SHORT).show();
        }
    }

    private void openOverlaySettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, "当前系统不需要单独授权悬浮窗", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "请开启“凡人指令自动填入”", Toast.LENGTH_LONG).show();
    }

    private void toggleFloatingButton() {
        boolean enabled = isFloatingEnabled();
        if (enabled) {
            setFloatingEnabled(false);
            FloatingSwitchService.stop(this);
            syncFloatingButtonState();
            return;
        }
        if (!canDrawOverlays()) {
            Toast.makeText(this, "请先授权悬浮按钮", Toast.LENGTH_SHORT).show();
            openOverlaySettings();
            return;
        }
        if (!AccessibilityServiceState.isEnabled(this)) {
            Toast.makeText(this, "建议同时授权无障碍自动填入，否则在其他应用中只能复制指令", Toast.LENGTH_LONG).show();
        }
        setFloatingEnabled(true);
        FloatingSwitchService.start(this);
        syncFloatingButtonState();
    }

    private void syncFloatingButtonState() {
        if (floatingButtonToggle == null) {
            return;
        }
        boolean enabled = isFloatingEnabled();
        if (enabled && canDrawOverlays()) {
            FloatingSwitchService.start(this);
            floatingButtonToggle.setText("关闭悬浮按钮");
        } else {
            if (enabled) {
                setFloatingEnabled(false);
            }
            floatingButtonToggle.setText("开启悬浮按钮");
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private boolean isFloatingEnabled() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(FLOATING_ENABLED, false);
    }

    private void setFloatingEnabled(boolean enabled) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(FLOATING_ENABLED, enabled)
                .apply();
    }
}
