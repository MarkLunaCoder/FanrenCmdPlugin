package com.mark.fanreninput;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.lang.ref.WeakReference;
import java.util.List;

public class FanRenAccessibilityService extends AccessibilityService {
    private static WeakReference<FanRenAccessibilityService> activeService =
            new WeakReference<>(null);

    private WeakReference<AccessibilityNodeInfo> lastExternalInputNode =
            new WeakReference<>(null);
    private String lastExternalInputPackageName;

    static boolean isRunning() {
        return activeService.get() != null;
    }

    static CommitResult commitCommandToActiveInput(String command) {
        FanRenAccessibilityService service = activeService.get();
        if (service == null) {
            return CommitResult.SERVICE_UNAVAILABLE;
        }
        return service.commitCommandInternal(command);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        activeService = new WeakReference<>(this);
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
    }

    @Override
    public void onDestroy() {
        if (activeService.get() == this) {
            activeService = new WeakReference<>(null);
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        rememberExternalInputNode(event);
    }

    @Override
    public void onInterrupt() {
    }

    private CommitResult commitCommandInternal(String command) {
        AccessibilityNodeInfo inputNode = findActiveInputNode();
        if (inputNode == null) {
            return CommitResult.INPUT_NOT_FOUND;
        }

        inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        inputNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        copyToClipboard(command);
        if (inputNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
            return CommitResult.SUCCESS;
        }

        CharSequence existingText = inputNode.getText();
        String fallbackText = (existingText == null ? "" : existingText.toString()) + command;
        Bundle arguments = new Bundle();
        arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                fallbackText
        );
        return inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                ? CommitResult.SUCCESS
                : CommitResult.ACTION_FAILED;
    }

    private AccessibilityNodeInfo findActiveInputNode() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        CharSequence activePackageName = packageNameOf(root);
        AccessibilityNodeInfo inputNode = findEditableNodeFromExternalTree(root);
        if (inputNode != null) {
            rememberInputNode(inputNode, packageNameOf(inputNode, activePackageName));
            return inputNode;
        }

        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows != null) {
            for (AccessibilityWindowInfo window : windows) {
                if (!window.isActive() && !window.isFocused()) {
                    continue;
                }
                inputNode = findEditableNodeFromExternalTree(window.getRoot());
                if (inputNode != null) {
                    rememberInputNode(inputNode, packageNameOf(inputNode));
                    return inputNode;
                }
            }

            for (AccessibilityWindowInfo window : windows) {
                inputNode = findEditableNodeFromExternalTree(window.getRoot());
                if (inputNode != null) {
                    rememberInputNode(inputNode, packageNameOf(inputNode));
                    return inputNode;
                }
            }
        }
        return findRememberedInputNode(activePackageName);
    }

    private void rememberExternalInputNode(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        boolean externalEvent =
                InputTargetPolicy.isExternalPackage(event.getPackageName(), getPackageName());
        if (!externalEvent) {
            return;
        }
        AccessibilityNodeInfo source = event.getSource();
        AccessibilityNodeInfo inputNode = findEditableNode(source);
        if (inputNode != null) {
            rememberInputNode(inputNode, packageNameOf(inputNode, event.getPackageName()));
            return;
        }
        if (InputTargetPolicy.shouldClearRememberedTarget(
                true,
                event.getEventType(),
                false
        )) {
            clearRememberedInputNode();
        }
    }

    private AccessibilityNodeInfo findEditableNodeFromExternalTree(AccessibilityNodeInfo root) {
        if (!InputTargetPolicy.isExternalPackage(packageNameOf(root), getPackageName())) {
            return null;
        }
        return findEditableNode(root);
    }

    private AccessibilityNodeInfo findRememberedInputNode(CharSequence activePackageName) {
        if (!InputTargetPolicy.canUseRememberedTarget(
                activePackageName,
                lastExternalInputPackageName,
                getPackageName()
        )) {
            return null;
        }
        AccessibilityNodeInfo rememberedInputNode = lastExternalInputNode.get();
        if (rememberedInputNode == null || !rememberedInputNode.refresh()) {
            clearRememberedInputNode();
            return null;
        }
        if (isUsableInputNode(rememberedInputNode)) {
            return rememberedInputNode;
        }
        clearRememberedInputNode();
        return null;
    }

    private void rememberInputNode(
            AccessibilityNodeInfo inputNode,
            CharSequence packageName
    ) {
        if (!isUsableInputNode(inputNode)
                || !InputTargetPolicy.isExternalPackage(packageName, getPackageName())) {
            return;
        }
        lastExternalInputNode = new WeakReference<>(inputNode);
        lastExternalInputPackageName = packageName.toString();
    }

    private void clearRememberedInputNode() {
        lastExternalInputNode = new WeakReference<>(null);
        lastExternalInputPackageName = null;
    }

    private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        AccessibilityNodeInfo focusedNode = node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (isUsableInputNode(focusedNode)) {
            return focusedNode;
        }
        if (isUsableInputNode(node)) {
            return node;
        }
        for (int index = node.getChildCount() - 1; index >= 0; index--) {
            AccessibilityNodeInfo child = node.getChild(index);
            AccessibilityNodeInfo result = findEditableNode(child);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private boolean isUsableInputNode(AccessibilityNodeInfo node) {
        return node != null
                && node.isEditable()
                && node.isEnabled()
                && node.isVisibleToUser()
                && !node.isPassword();
    }

    private CharSequence packageNameOf(AccessibilityNodeInfo node) {
        return packageNameOf(node, null);
    }

    private CharSequence packageNameOf(
            AccessibilityNodeInfo node,
            CharSequence fallbackPackageName
    ) {
        return node == null || node.getPackageName() == null
                ? fallbackPackageName
                : node.getPackageName();
    }

    private void copyToClipboard(String command) {
        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("fanren_command", command));
        }
    }

    enum CommitResult {
        SUCCESS,
        SERVICE_UNAVAILABLE,
        INPUT_NOT_FOUND,
        ACTION_FAILED
    }
}
