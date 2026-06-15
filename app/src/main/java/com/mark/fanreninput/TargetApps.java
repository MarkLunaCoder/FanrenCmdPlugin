package com.mark.fanreninput;

final class TargetApps {
    private TargetApps() {
    }

    static boolean isTelegramPackage(CharSequence packageName) {
        if (packageName == null) {
            return false;
        }
        return isTelegramPackage(packageName.toString());
    }

    static boolean isTelegramPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return packageName.equals("org.telegram.messenger")
                || packageName.equals("org.telegram.messenger.web")
                || packageName.equals("org.telegram.plus")
                || packageName.equals("org.thunderdog.challegram");
    }
}
