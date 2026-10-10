package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.uidata.UiDataModel.Tab.MASJID;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.batoulapps.adhan.CalculationMethod;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.SalarmFragment;
import com.qwibbler.salarm.dialogfragment.MaterialTimePickerDialogFragment;
import com.qwibbler.salarm.dialogfragment.SpinnerTimePickerDialogFragment;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.settings.MasjidEditDialogFragment;
import com.qwibbler.salarm.settings.PreferencesKeys;
import com.qwibbler.salarm.settings.SalahMasjidAlarmDialogFragment;
import static com.qwibbler.salarm.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;
import com.google.android.material.color.MaterialColors;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MasjidFragment extends SalarmFragment {

    private View mEmptyStateLayout;
    private View mContentLayout;
    private View mMultipleMasjidsHeader;
    private View mMultipleMasjidsDivider;
    private TextView mCurrentMasjidDropdownText;
    private View mMasjidDropdownTrigger;
    private View mPrevMasjidButton;
    private View mNextMasjidButton;
    private View mMasjidTitleContainer;
    private TextView mMasjidTitleText;
    private RecyclerView mRecyclerView;
    private MasjidPrayerAdapter mAdapter;

    private List<Masjid> mMasjids = new ArrayList<>();
    private long mSelectedMasjidId = -1;

    public MasjidFragment() {
        super(MASJID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.masjid_fragment, container, false);
        mEmptyStateLayout = view.findViewById(R.id.masjid_empty_state_layout);
        mContentLayout = view.findViewById(R.id.masjid_content_layout);
        mMultipleMasjidsHeader = view.findViewById(R.id.multiple_masjids_header);
        mMultipleMasjidsDivider = view.findViewById(R.id.multiple_masjids_divider);
        mCurrentMasjidDropdownText = view.findViewById(R.id.current_masjid_dropdown_text);
        mMasjidDropdownTrigger = view.findViewById(R.id.masjid_dropdown_trigger);
        mPrevMasjidButton = view.findViewById(R.id.prev_masjid_button);
        mNextMasjidButton = view.findViewById(R.id.next_masjid_button);
        mMasjidTitleContainer = view.findViewById(R.id.masjid_title_container);
        mMasjidTitleText = view.findViewById(R.id.masjid_title_text);
        mRecyclerView = view.findViewById(R.id.masjid_prayer_list);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new MasjidPrayerAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mPrevMasjidButton.setOnClickListener(v -> shiftMasjid(-1));
        mNextMasjidButton.setOnClickListener(v -> shiftMasjid(1));
        mMasjidDropdownTrigger.setOnClickListener(v -> showMasjidPickerDialog());

        getParentFragmentManager().setFragmentResultListener("masjid_updated", this, (requestKey, result) -> loadMasjids());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMasjids();
    }

    private void loadMasjids() {
        Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            List<Masjid> masjids = SalahDatabase.getDatabase(appContext).masjidDao().getAll();
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    mMasjids = masjids;
                    updateUI();
                }
            });
        });
    }

    private void updateUI() {
        Context context = getContext();
        if (context == null) return;

        if (mMasjids.isEmpty()) {
            mEmptyStateLayout.setVisibility(View.VISIBLE);
            mContentLayout.setVisibility(View.GONE);
            return;
        }

        mEmptyStateLayout.setVisibility(View.GONE);
        mContentLayout.setVisibility(View.VISIBLE);

        boolean found = false;
        for (Masjid m : mMasjids) {
            if (m.id == mSelectedMasjidId) {
                found = true;
                break;
            }
        }
        if (!found) {
            mSelectedMasjidId = mMasjids.get(0).id;
        }

        Masjid currentMasjid = null;
        for (Masjid m : mMasjids) {
            if (m.id == mSelectedMasjidId) {
                currentMasjid = m;
                break;
            }
        }
        if (currentMasjid == null) currentMasjid = mMasjids.get(0);

        final Masjid masjid = currentMasjid;
        mMasjidTitleText.setText(masjid.name);
        mMasjidTitleContainer.setOnClickListener(v -> MasjidEditDialogFragment.show(getParentFragmentManager(), masjid.id));

        if (mMasjids.size() > 1) {
            mMultipleMasjidsHeader.setVisibility(View.VISIBLE);
            mMultipleMasjidsDivider.setVisibility(View.VISIBLE);
            mCurrentMasjidDropdownText.setText(masjid.name);
        } else {
            mMultipleMasjidsHeader.setVisibility(View.GONE);
            mMultipleMasjidsDivider.setVisibility(View.GONE);
        }

        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            List<PrayerItem> items = new ArrayList<>();
            String[] types = {"FAJR", "DHUHR", "JUMA", "ASR", "MAGHRIB", "ISHA"};

            SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(appContext);
            String latStr = prefs.getString(PreferencesKeys.KEY_SALAH_LATITUDE, "0.0");
            String lonStr = prefs.getString(PreferencesKeys.KEY_SALAH_LONGITUDE, "0.0");
            double lat = 0, lon = 0;
            try {
                lat = Double.parseDouble(latStr);
                lon = Double.parseDouble(lonStr);
            } catch (NumberFormatException ignored) {}

            String methodStr = prefs.getString(PreferencesKeys.KEY_SALAH_CALC_METHOD, CalculationMethod.MUSLIM_WORLD_LEAGUE.name());
            CalculationMethod method = CalculationMethod.valueOf(methodStr);
            String madhab = prefs.getString(PreferencesKeys.KEY_SALAH_MADHAB, "HANAFI");

            SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), lat, lon, method);

            for (String type : types) {
                MasjidEvent event = db.masjidEventDao().getMasjidEvent(masjid.id, type);
                long timeMillis = 0;
                String summary = "";
                boolean isConfigured = false;

                if (event != null) {
                    timeMillis = SalahCalculator.evaluateMasjidEvent(appContext, event, st, new Date());
                    if (timeMillis > 0) {
                        isConfigured = true;
                        String anchorName = RelationalRule.getAnchorDescription(appContext, event.definition.anchor);
                        summary = event.definition.getSummary(appContext, anchorName, true);
                    }
                }

                if (!isConfigured) {
                    Date baseDate = switch (type) {
                        case "FAJR" -> st.fajr;
                        case "DHUHR", "JUMA" -> st.dhuhr;
                        case "ASR" -> "HANAFI".equals(madhab) ? st.asrHanafi : st.asrShafi;
                        case "MAGHRIB" -> st.maghrib;
                        case "ISHA" -> st.isha;
                        default -> null;
                    };
                    if (baseDate != null) {
                        timeMillis = baseDate.getTime();
                    }
                }

                items.add(new PrayerItem(type, SalahUtils.getLocalizedSalahName(appContext, type), timeMillis, summary, isConfigured, masjid));
            }

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    mAdapter.setItems(items);
                }
            });
        });
    }

    private void shiftMasjid(int delta) {
        if (mMasjids.isEmpty()) return;
        int currentIndex = 0;
        for (int i = 0; i < mMasjids.size(); i++) {
            if (mMasjids.get(i).id == mSelectedMasjidId) {
                currentIndex = i;
                break;
            }
        }
        int newIndex = (currentIndex + delta + mMasjids.size()) % mMasjids.size();
        mSelectedMasjidId = mMasjids.get(newIndex).id;
        updateUI();
    }

    private void showMasjidPickerDialog() {
        if (mMasjids.isEmpty()) return;
        CharSequence[] names = new CharSequence[mMasjids.size()];
        for (int i = 0; i < mMasjids.size(); i++) {
            names[i] = mMasjids.get(i).name;
        }

        int checkedItem = 0;
        for (int i = 0; i < mMasjids.size(); i++) {
            if (mMasjids.get(i).id == mSelectedMasjidId) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.salah_masjids_category_title)
                .setSingleChoiceItems(names, checkedItem, (dialog, which) -> {
                    mSelectedMasjidId = mMasjids.get(which).id;
                    updateUI();
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setImageResource(R.drawable.ic_add);
        fab.setVisibility(View.VISIBLE);
        fab.setOnLongClickListener(v -> true);
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        left.setVisibility(View.GONE);
        right.setVisibility(View.GONE);
    }

    @Override
    public void onFabClick() {
        MasjidEditDialogFragment.show(getParentFragmentManager(), 0);
    }

    private static class PrayerItem {
        String type;
        String name;
        long timeMillis;
        String summary;
        boolean isConfigured;
        Masjid masjid;

        PrayerItem(String type, String name, long timeMillis, String summary, boolean isConfigured, Masjid masjid) {
            this.type = type;
            this.name = name;
            this.timeMillis = timeMillis;
            this.summary = summary;
            this.isConfigured = isConfigured;
            this.masjid = masjid;
        }
    }

    private class MasjidPrayerAdapter extends RecyclerView.Adapter<MasjidPrayerAdapter.ViewHolder> {
        private List<PrayerItem> mItems = new ArrayList<>();

        void setItems(final List<PrayerItem> items) {
            final List<PrayerItem> oldItems = mItems;
            mItems = items;
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldItems.size();
                }

                @Override
                public int getNewListSize() {
                    return mItems.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldItems.get(oldItemPosition).type.equals(mItems.get(newItemPosition).type);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    PrayerItem oldItem = oldItems.get(oldItemPosition);
                    PrayerItem newItem = mItems.get(newItemPosition);
                    return oldItem.type.equals(newItem.type) &&
                            oldItem.timeMillis == newItem.timeMillis &&
                            Objects.equals(oldItem.summary, newItem.summary) &&
                            oldItem.isConfigured == newItem.isConfigured;
                }
            }).dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.masjid_prayer_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PrayerItem item = mItems.get(position);
            holder.name.setText(item.name);

            if (item.isConfigured && item.summary != null && !item.summary.isEmpty()) {
                holder.summary.setText(item.summary);
                holder.summary.setVisibility(View.VISIBLE);
                holder.time.setTextColor(MaterialColors.getColor(holder.itemView, android.R.attr.textColorPrimary));
            } else {
                holder.summary.setVisibility(View.GONE);
                int secondaryColor = MaterialColors.getColor(holder.itemView, android.R.attr.textColorSecondary, Color.GRAY);
                holder.time.setTextColor(secondaryColor);
            }

            if (item.timeMillis > 0) {
                Date date = new Date(item.timeMillis);
                DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getContext());
                holder.time.setText(timeFormat.format(date));
            } else {
                holder.time.setText("--:--");
            }

            holder.itemView.setOnClickListener(v -> {
                if ("DHUHR".equals(item.type) || "JUMA".equals(item.type)) {
                    showFixedTimeDialogue(item.masjid, item.type);
                } else {
                    SalahMasjidAlarmDialogFragment.show(getParentFragmentManager(), item.masjid.id, item.type);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView summary;
            TextView time;

            ViewHolder(View view) {
                super(view);
                name = view.findViewById(R.id.prayer_name);
                summary = view.findViewById(R.id.prayer_calc_summary);
                time = view.findViewById(R.id.prayer_time);
            }
        }
    }

    private void showFixedTimeDialogue(Masjid masjid, String type) {
        Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        if (!"DHUHR".equals(type) && !"JUMA".equals(type)) {
            return;
        }

        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            MasjidEvent event = db.masjidEventDao().getMasjidEvent(masjid.id, type);
            if (event == null) {
                event = new MasjidEvent();
                event.masjidId = masjid.id;
                event.salahType = type;
                event.definition.isFixedTime = true;
                event.isSelected = masjid.showMasjidEvents;

                double[] coords = SalahUtils.getCoordinates(appContext);
                SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], SalahUtils.getCalculationMethod(appContext));
                Date prayerTime = switch (type) {
                    case "DHUHR", "JUMA" -> st.dhuhr;
                    default -> null;
                };

                if (prayerTime != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(prayerTime);
                    event.definition.fixedHour = cal.get(Calendar.HOUR_OF_DAY);
                    event.definition.fixedMinute = cal.get(Calendar.MINUTE);
                } else {
                    event.definition.fixedHour = 12;
                    event.definition.fixedMinute = 0;
                }
                event.id = db.masjidEventDao().insert(event);
            } else if (!event.definition.isFixedTime) {
                double[] coords = SalahUtils.getCoordinates(appContext);
                SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], SalahUtils.getCalculationMethod(appContext));

                long evaluated = SalahCalculator.evaluateMasjidEvent(appContext, event, st, new Date());
                if (evaluated > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(evaluated);
                    event.definition.fixedHour = cal.get(Calendar.HOUR_OF_DAY);
                    event.definition.fixedMinute = cal.get(Calendar.MINUTE);
                } else {
                    event.definition.fixedHour = 12;
                    event.definition.fixedMinute = 0;
                }
                event.definition.isFixedTime = true;
                db.masjidEventDao().update(event);
            }

            final MasjidEvent finalEvent = event;

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    showTimePicker(finalEvent);
                }
            });
        });
    }

    private void showTimePicker(MasjidEvent event) {
        SharedPreferences prefs = SalarmApplication.getDefaultSharedPreferences(requireContext());

        if (SPINNER_TIME_PICKER_STYLE.equals(SettingsDAO.getMaterialTimePickerStyle(prefs))) {
            final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(event.definition.fixedHour, event.definition.fixedMinute);
            getChildFragmentManager().setFragmentResultListener(SpinnerTimePickerDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
                int h = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                updateEventTime(event, h, m);
            });
            SpinnerTimePickerDialogFragment.show(getChildFragmentManager(), fragment);
        } else {
            getChildFragmentManager().setFragmentResultListener(MaterialTimePickerDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
                int h = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                updateEventTime(event, h, m);
            });
            MaterialTimePickerDialogFragment.show(requireContext(), getChildFragmentManager(), "fixed_time_picker", event.definition.fixedHour, event.definition.fixedMinute, prefs);
        }
    }

    private void updateEventTime(MasjidEvent event, int hour, int minute) {
        event.definition.fixedHour = hour;
        event.definition.fixedMinute = minute;
        Context context = getContext();
        if (context == null) return;
        final Context appContext = context.getApplicationContext();
        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            db.masjidEventDao().update(event);

            SalahAlarmSyncManager.syncSelectedRulesToAlarms(appContext);
            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    loadMasjids();
                }
            });
        });
    }
}
