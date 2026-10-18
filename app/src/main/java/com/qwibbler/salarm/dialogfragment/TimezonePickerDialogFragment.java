/*
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.dialogfragment;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;

import com.qwibbler.salarm.R;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.data.TimeZones;
import com.qwibbler.salarm.uicomponents.CustomDialog;

import java.util.ArrayList;
import java.util.List;

public class TimezonePickerDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "timezone_picker_request";
    public static final String RESULT_TIMEZONE_ID = "result_timezone_id";

    public static TimezonePickerDialogFragment newInstance() {
        return new TimezonePickerDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.alarm_timezone_picker_dialog, null);

        SearchView searchView = view.findViewById(R.id.timezone_search_view);
        ListView listView = view.findViewById(R.id.timezone_list_view);

        View searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        TimeZones timeZones = SettingsDAO.getTimeZones(requireContext(), System.currentTimeMillis());
        CharSequence[] ids = timeZones.timeZoneIds();
        CharSequence[] names = timeZones.timeZoneNames();

        List<TimezoneItem> allItems = new ArrayList<>();
        for (int idx = 0; idx < ids.length; idx++) {
            String tzId = ids[idx].toString();
            String tzName = names[idx].toString();
            allItems.add(new TimezoneItem(tzId, tzName));
        }

        ArrayAdapter<TimezoneItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, new ArrayList<>(allItems));
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id1) -> {
            TimezoneItem selected = adapter.getItem(position);
            if (selected != null) {
                Bundle result = new Bundle();
                result.putString(RESULT_TIMEZONE_ID, selected.id);
                getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                dismiss();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

        return CustomDialog.create(
            requireContext(),
            null,
            null,
            getString(R.string.select_timezone_dialog_title),
            null,
            view,
            null,
            null,
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.NONE
        );
    }

    private static class TimezoneItem {
        final String id;
        final String displayName;

        TimezoneItem(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        @NonNull
        @Override
        public String toString() {
            return displayName;
        }
    }
}
