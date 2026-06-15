package com.mark.fanreninput;

import android.view.accessibility.AccessibilityEvent;

final class InputTargetPolicy {
    private InputTargetPolicy() {
    }

    static boolean isExternalPackage(CharSequence packageName, String ownPackageName) {
        String candidate = normalize(packageName);
        String ownPackage = normalize(ownPackageName);
        if (candidate.isEmpty() || ownPackage.isEmpty()) {
            return false;
        }
        return !candidate.equals(ownPackage);
    }

    static boolean canUseRememberedTarget(
            CharSequence currentPackageName,
            CharSequence rememberedPackageName,
            String ownPackageName
    ) {
        if (!isExternalPackage(rememberedPackageName, ownPackageName)) {
            return false;
        }
        String currentPackage = normalize(currentPackageName);
        if (currentPackage.isEmpty()) {
            return true;
        }
        String ownPackage = normalize(ownPackageName);
        String rememberedPackage = normalize(rememberedPackageName);
        return currentPackage.equals(ownPackage) || currentPackage.equals(rememberedPackage);
    }

    static boolean shouldClearRememberedTarget(
            boolean externalEvent,
            int eventType,
            boolean hasEditableInput
    ) {
        if (!externalEvent || hasEditableInput) {
            return false;
        }
        return eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                || eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
    }

    private static String normalize(CharSequence packageName) {
        return packageName == null ? "" : packageName.toString().trim();
    }
}
