// SPDX-License-Identifier: GPL-3.0-only

package com.qwibbler.salarm.tiles;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.qwibbler.salarm.SalarmApplication.getDefaultSharedPreferences;
import static com.qwibbler.salarm.uidata.UiDataModel.Tab.ALARMS;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import com.qwibbler.salarm.SalarmActivity;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.alarms.AlarmStateManager;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.uidata.UiDataModel;
import com.qwibbler.salarm.utils.AlarmUtils;
import com.qwibbler.salarm.utils.SdkUtils;

@RequiresApi(api = Build.VERSION_CODES.N)
public class AlarmTileService extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile(getQsTile());
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override
    public void onClick() {
        super.onClick();

        final Intent intent = new Intent(this, SalarmActivity.class)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
            .addFlags(FLAG_ACTIVITY_CLEAR_TOP);

        UiDataModel.getUiDataModel().setSelectedTab(ALARMS);

        if (SdkUtils.isAtLeastAndroid14()) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(intent);
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        updateTile(getQsTile());
    }

    public void onStopListening() {
        super.onStopListening();

        updateTile(getQsTile());
    }

    private void updateTile(Tile tile) {
        if (tile == null) {
            return;
        }

        SharedPreferences prefs = getDefaultSharedPreferences(this);
        if (!SettingsDAO.isAlarmTabVisible(prefs)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            if (SdkUtils.isAtLeastAndroid10()) {
                tile.setSubtitle(null);
            }

            tile.updateTile();
            return;
        }

        if (AlarmStateManager.getNextFiringAlarm(this) == null) {
            tile.setState(Tile.STATE_INACTIVE);
            if (SdkUtils.isAtLeastAndroid10()) {
                tile.setSubtitle(getString(R.string.no_scheduled_alarms));
            }
        } else {
            tile.setState(Tile.STATE_ACTIVE);
            if (SdkUtils.isAtLeastAndroid10()) {
                tile.setSubtitle(AlarmUtils.getNextAlarm(this));
            }
        }

        tile.updateTile();
    }
}
