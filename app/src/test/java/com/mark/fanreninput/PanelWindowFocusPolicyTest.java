package com.mark.fanreninput;

import android.view.WindowManager;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PanelWindowFocusPolicyTest {
    @Test
    public void commandModeDoesNotTakeInputFocus() {
        int flags = PanelWindowFocusPolicy.flagsForInputFocus(false);

        assertTrue((flags & WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) != 0);
        assertTrue((flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0);
    }

    @Test
    public void searchModeCanTakeInputFocus() {
        int flags = PanelWindowFocusPolicy.flagsForInputFocus(true);

        assertTrue((flags & WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) != 0);
        assertFalse((flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0);
    }

    @Test
    public void searchModeDoesNotBlockOutsideTouches() {
        int flags = PanelWindowFocusPolicy.flagsForInputFocus(true);

        assertTrue((flags & WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) != 0);
    }
}
