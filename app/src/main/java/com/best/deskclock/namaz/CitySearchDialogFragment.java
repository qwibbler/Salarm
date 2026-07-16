package com.best.deskclock.namaz;

import android.app.Dialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitySearchDialogFragment extends DialogFragment {

    private List<Address> mAddresses = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.namaz_city_search_dialog, container, false);

        SearchView searchView = view.findViewById(R.id.city_search_view);
        ListView listView = view.findViewById(R.id.city_results_list);

        mAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(mAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2) {
                    performSearch(newText);
                }
                return true;
            }
        });

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            if (position < mAddresses.size()) {
                Address address = mAddresses.get(position);
                String cityName = mAdapter.getItem(position);
                NamazLocationManager.handleNewLocation(requireContext(), address.getLatitude(), address.getLongitude(), false);
                // Also update the city name in preferences manually since it was not GPS
                requireContext().getSharedPreferences(requireContext().getPackageName() + "_preferences", 0)
                        .edit()
                        .putString("key_namaz_city_name", cityName)
                        .apply();
                dismiss();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    private void performSearch(String query) {
        AppExecutors.getDiskIO().execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 10);
                AppExecutors.getMainThread().post(() -> {
                    if (addresses != null) {
                        mAddresses = addresses;
                        List<String> names = new ArrayList<>();
                        for (Address addr : addresses) {
                            String name = addr.getLocality();
                            if (name == null) name = addr.getSubAdminArea();
                            if (name == null) name = addr.getAdminArea();
                            if (name == null) name = addr.getFeatureName();

                            String country = addr.getCountryName();
                            names.add((name != null ? name : "Unknown") + (country != null ? ", " + country : ""));
                        }
                        mAdapter.clear();
                        mAdapter.addAll(names);
                        mAdapter.notifyDataSetChanged();
                    }
                });
            } catch (IOException e) {
                LogUtils.e("Search failed", e);
            }
        });
    }
}
