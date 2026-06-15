package com.mark.fanreninput;

import android.view.WindowManager;

final class PanelWindowFocusPolicy {
    private PanelWindowFocusPolicy() {
    }

    static int flagsForInputFocus(boolean allowInputFocus) {
        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        if (!allowInputFocus) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        return flags;
    }
}
