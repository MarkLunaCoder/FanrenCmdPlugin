package com.mark.fanreninput;

import org.junit.Test;

import android.view.accessibility.AccessibilityEvent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputTargetPolicyTest {
    @Test
    public void allowsInputTargetsFromOtherPackages() {
        assertTrue(InputTargetPolicy.isExternalPackage("com.miui.notes", "com.mark.fanreninput"));
    }

    @Test
    public void rejectsInputTargetsFromOwnPackage() {
        assertFalse(InputTargetPolicy.isExternalPackage("com.mark.fanreninput", "com.mark.fanreninput"));
    }

    @Test
    public void rejectsMissingPackageNames() {
        assertFalse(InputTargetPolicy.isExternalPackage(null, "com.mark.fanreninput"));
        assertFalse(InputTargetPolicy.isExternalPackage("", "com.mark.fanreninput"));
    }

    @Test
    public void rememberedExternalTargetCanBeUsedWhenCurrentWindowIsOwnPackage() {
        assertTrue(InputTargetPolicy.canUseRememberedTarget(
                "com.mark.fanreninput",
                "com.miui.notes",
                "com.mark.fanreninput"
        ));
    }

    @Test
    public void rememberedExternalTargetCannotCrossToAnotherExternalPackage() {
        assertFalse(InputTargetPolicy.canUseRememberedTarget(
                "org.telegram.messenger",
                "com.miui.notes",
                "com.mark.fanreninput"
        ));
    }

    @Test
    public void externalFocusMovingToNonInputClearsRememberedTarget() {
        assertTrue(InputTargetPolicy.shouldClearRememberedTarget(
                true,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                false
        ));
    }

    @Test
    public void externalContentChangeWithoutInputDoesNotClearRememberedTarget() {
        assertFalse(InputTargetPolicy.shouldClearRememberedTarget(
                true,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                false
        ));
    }
}
