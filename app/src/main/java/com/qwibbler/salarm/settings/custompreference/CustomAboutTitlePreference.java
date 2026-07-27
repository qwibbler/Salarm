// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.settings.custompreference;

import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.databinding.SettingsAboutTitleBinding;
import com.qwibbler.salarm.utils.ThemeUtils;

/**
 * A {@link Preference} with a custom layout applied to the title in About.
 */
public class CustomAboutTitlePreference extends Preference {

    private final Typeface mRegularTypeface;

    public CustomAboutTitlePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mRegularTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(context)));
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            return;
        }

        super.onBindViewHolder(holder);

        SettingsAboutTitleBinding binding = SettingsAboutTitleBinding.bind(holder.itemView);

        binding.slogan.setTypeface(mRegularTypeface);
    }

}
