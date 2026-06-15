package com.mark.fanreninput;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

final class AccessibilityServiceState {
    private AccessibilityServiceState() {
    }

    static boolean isEnabled(Context context) {
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabledServices == null) {
            return false;
        }
        String expectedService = new ComponentName(
                context,
                FanRenAccessibilityService.class
        ).flattenToString();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        for (String service : splitter) {
            if (expectedService.equalsIgnoreCase(service)) {
                return true;
            }
        }
        return false;
    }
}
