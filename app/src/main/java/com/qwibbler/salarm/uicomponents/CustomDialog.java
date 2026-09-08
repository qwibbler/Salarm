// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.uicomponents;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Space;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.DialogCustomLayoutBinding;
import com.qwibbler.salarm.databinding.DialogMessageCustomBinding;
import com.qwibbler.salarm.databinding.DialogTitleCustomBinding;
import com.qwibbler.salarm.utils.ThemeUtils;
import com.qwibbler.salarm.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CustomDialog {

    public static AlertDialog create(
        @NonNull Context context,
        @Nullable @StyleRes Integer styleRes,
        @Nullable Drawable icon,
        @Nullable CharSequence title,
        @Nullable CharSequence message,
        @Nullable View customView,
        @Nullable CharSequence positiveText,
        @Nullable DialogInterface.OnClickListener positiveListener,
        @Nullable CharSequence negativeText,
        @Nullable DialogInterface.OnClickListener negativeListener,
        @Nullable CharSequence neutralText,
        @Nullable DialogInterface.OnClickListener neutralListener,
        @Nullable OnDialogReady onDialogReady,
        @NonNull SoftInputMode softInputMode
    ) {
        List<DialogButton> buttons = new ArrayList<>();
        if (neutralText != null) buttons.add(new DialogButton(neutralText, neutralListener));
        if (negativeText != null) buttons.add(new DialogButton(negativeText, negativeListener));
        if (positiveText != null) buttons.add(new DialogButton(positiveText, positiveListener));
        return create(context, styleRes, icon, title, message, customView, onDialogReady, softInputMode, buttons.toArray(new DialogButton[0]));
    }

    public static AlertDialog create(
        @NonNull Context context,
        @Nullable @StyleRes Integer styleRes,
        @Nullable Drawable icon,
        @Nullable CharSequence title,
        @Nullable CharSequence message,
        @Nullable View customView,
        @Nullable CharSequence positiveText,
        @Nullable DialogInterface.OnClickListener positiveListener,
        @Nullable CharSequence negativeText,
        @Nullable DialogInterface.OnClickListener negativeListener,
        @Nullable CharSequence neutralText,
        @Nullable DialogInterface.OnClickListener neutralListener,
        @Nullable CharSequence copyText,
        @Nullable View.OnClickListener copyListener,
        @Nullable OnDialogReady onDialogReady,
        @NonNull SoftInputMode softInputMode
    ) {
        List<DialogButton> buttons = new ArrayList<>();
        if (neutralText != null) buttons.add(new DialogButton(neutralText, neutralListener));
        if (negativeText != null) buttons.add(new DialogButton(negativeText, negativeListener));
        if (positiveText != null) buttons.add(new DialogButton(positiveText, positiveListener));
        if (copyText != null) buttons.add(new DialogButton(copyText, (d, w) -> {
            if (copyListener != null) copyListener.onClick(null);
        }));
        return create(context, styleRes, icon, title, message, customView, onDialogReady, softInputMode, buttons.toArray(new DialogButton[0]));
    }

    public static AlertDialog create(
        @NonNull Context context,
        @Nullable @StyleRes Integer styleRes,
        @Nullable Drawable icon,
        @Nullable CharSequence title,
        @Nullable CharSequence message,
        @Nullable View customView,
        @Nullable OnDialogReady onDialogReady,
        @NonNull SoftInputMode softInputMode,
        DialogButton... buttons
    ) {

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        // Builder
        MaterialAlertDialogBuilder builder = (styleRes != null)
            ? new MaterialAlertDialogBuilder(context, styleRes)
            : new MaterialAlertDialogBuilder(context);

        // Title and icon
        if (title != null || icon != null) {
            DialogTitleCustomBinding titleBinding = DialogTitleCustomBinding.inflate(LayoutInflater.from(context));

            if (icon != null) {
                titleBinding.dialogTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                titleBinding.dialogTitle.setCompoundDrawablePadding((int) dpToPx(18, context.getResources().getDisplayMetrics()));
            }

            if (title != null) {
                titleBinding.dialogTitle.setText(title);
                titleBinding.dialogTitle.setTypeface(typeface);
            }

            builder.setCustomTitle(titleBinding.getRoot());
        }

        // Custom Layout for Content and Buttons
        DialogCustomLayoutBinding layoutBinding = DialogCustomLayoutBinding.inflate(LayoutInflater.from(context));
        View dialogViewRoot = layoutBinding.getRoot();

        // Content (Message or Custom View)
        View content = null;
        if (message != null) {
            DialogMessageCustomBinding messageBinding = DialogMessageCustomBinding.inflate(LayoutInflater.from(context));
            messageBinding.dialogMessage.setText(message);
            messageBinding.dialogMessage.setTypeface(typeface);
            content = messageBinding.getRoot();
        } else if (customView != null) {
            content = customView;
        }

        if (content != null) {
            if (content.getParent() != null) {
                ((android.view.ViewGroup) content.getParent()).removeView(content);
            }
            layoutBinding.customContentContainer.addView(content);
        }

        builder.setView(dialogViewRoot);
        AlertDialog dialog = builder.create();

        // Action Buttons
        if (buttons != null && buttons.length > 0) {
            for (int i = 0; i < buttons.length; i++) {
                final DialogButton dialogButton = buttons[i];
                if (dialogButton == null || dialogButton.text == null) continue;

                com.google.android.material.button.MaterialButton button = new com.google.android.material.button.MaterialButton(context, null, android.R.attr.buttonBarButtonStyle);
                button.setText(dialogButton.text);
                button.setAllCaps(false);
                button.setTypeface(typeface);
                button.setOnClickListener(v -> {
                    if (dialogButton.listener != null) {
                        dialogButton.listener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                    }
                    if (dialogButton.dismissOnClick) {
                        dialog.dismiss();
                    }
                });

                // Add button with natural width
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutBinding.customButtonBar.addView(button, lp);

                // Add flexible spacer between buttons for "space-between" effect
                if (i < buttons.length - 1) {
                    Space spacer = new Space(context);
                    LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 0, 1f);
                    layoutBinding.customButtonBar.addView(spacer, spacerLp);
                }
            }
        } else {
            layoutBinding.customButtonBar.setVisibility(View.GONE);
        }

        final View finalContent = content;

        // Apply typeface and extra logic on show
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                Utils.setupTapToDismissKeyboard(dialog.getWindow());
            }

            if (finalContent != null && finalContent.findViewById(R.id.scroll_view) != null) {
                configureScrollView(finalContent);
            }

            if (onDialogReady != null) {
                onDialogReady.onReady(dialog);
            }
        });

        // Window configuration
        Window window = dialog.getWindow();
        if (window != null) {
            ThemeUtils.allowDisplayCutout(window);
            if (softInputMode == SoftInputMode.SHOW_KEYBOARD) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                );
            }
        }

        return dialog;
    }

    private static void configureScrollView(View dialogView) {
        NestedScrollView scrollView = dialogView.findViewById(R.id.scroll_view);
        if (scrollView == null) return;

        boolean scrollable = scrollView.canScrollVertically(1) || scrollView.canScrollVertically(-1);

        if (scrollable) {
            scrollView.setScrollIndicators(View.SCROLL_INDICATOR_BOTTOM);
        }

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            scrollView.setScrollIndicators(View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_BOTTOM);

            boolean atTop = !scrollView.canScrollVertically(-1);
            boolean atBottom = !scrollView.canScrollVertically(1);

            if (atTop) {
                scrollView.setScrollIndicators(View.SCROLL_INDICATOR_BOTTOM);
            }
            if (atBottom) {
                scrollView.setScrollIndicators(View.SCROLL_INDICATOR_TOP);
            }
        });
    }

    public record DialogButton(CharSequence text, DialogInterface.OnClickListener listener, boolean dismissOnClick) {
            public DialogButton(@Nullable CharSequence text, @Nullable DialogInterface.OnClickListener listener) {
                this(text, listener, true);
            }

            public DialogButton(@Nullable CharSequence text, @Nullable DialogInterface.OnClickListener listener, boolean dismissOnClick) {
                this.text = text;
                this.listener = listener;
                this.dismissOnClick = dismissOnClick;
            }
        }

    public interface OnDialogReady {
        void onReady(AlertDialog dialog);
    }

    public enum SoftInputMode {
        NONE,
        SHOW_KEYBOARD
    }

}
