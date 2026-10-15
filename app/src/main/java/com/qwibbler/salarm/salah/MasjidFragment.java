package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.uidata.UiDataModel.Tab.MASJID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.SalarmFragment;
import com.qwibbler.salarm.dialogfragment.MaterialTimePickerDialogFragment;
import com.qwibbler.salarm.dialogfragment.SpinnerTimePickerDialogFragment;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.salah.ui.MasjidCardEditorActivity;
import com.qwibbler.salarm.settings.MasjidEditDialogFragment;
import com.qwibbler.salarm.settings.SalahMasjidAlarmDialogFragment;
import com.qwibbler.salarm.uidata.TabListener;
import com.qwibbler.salarm.uidata.UiDataModel;
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
    private View mPrevMasjidButton;
    private View mNextMasjidButton;
    private View mMasjidTitleClickableArea;
    private TextView mMasjidTitleText;
    private RecyclerView mRecyclerView;
    private View mViewTimesheetButton;
    private MasjidPrayerAdapter mAdapter;

    private List<Masjid> mMasjids = new ArrayList<>();
    private long mSelectedMasjidId = -1;
    private boolean mShowCheckboxes = false;

    private final TabListener mTabListener = newSelectedTab -> {
        if (newSelectedTab != MASJID) {
            exitCheckboxMode();
        }
    };

    public MasjidFragment() {
        super(MASJID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.masjid_fragment, container, false);
        mEmptyStateLayout = view.findViewById(R.id.masjid_empty_state_layout);
        mContentLayout = view.findViewById(R.id.masjid_content_layout);
        mPrevMasjidButton = view.findViewById(R.id.prev_masjid_button);
        mNextMasjidButton = view.findViewById(R.id.next_masjid_button);
        mMasjidTitleClickableArea = view.findViewById(R.id.masjid_title_clickable_area);
        mMasjidTitleText = view.findViewById(R.id.masjid_title_text);
        mRecyclerView = view.findViewById(R.id.masjid_prayer_list);
        mViewTimesheetButton = view.findViewById(R.id.view_timesheet_button);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new MasjidPrayerAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mPrevMasjidButton.setOnClickListener(v -> shiftMasjid(-1));
        mPrevMasjidButton.setOnLongClickListener(v -> {
            showMasjidPickerDialog();
            return true;
        });

        mNextMasjidButton.setOnClickListener(v -> shiftMasjid(1));
        mNextMasjidButton.setOnLongClickListener(v -> {
            showMasjidPickerDialog();
            return true;
        });

        view.setOnClickListener(v -> exitCheckboxMode());
        mContentLayout.setOnClickListener(v -> exitCheckboxMode());

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    exitCheckboxMode();
                }
            }
        });

        mRecyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (mShowCheckboxes && e.getAction() == MotionEvent.ACTION_DOWN) {
                    View child = rv.findChildViewUnder(e.getX(), e.getY());
                    if (child == null) {
                        exitCheckboxMode();
                        return true;
                    } else {
                        int pos = rv.getChildAdapterPosition(child);
                        if (pos != RecyclerView.NO_POSITION) {
                            PrayerItem item = mAdapter.getItem(pos);
                            if (item != null && (!item.isConfigured || item.event == null)) {
                                exitCheckboxMode();
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });

        UiDataModel.getUiDataModel().addTabListener(mTabListener);

        getParentFragmentManager().setFragmentResultListener("masjid_updated", this, (requestKey, result) -> loadMasjids());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        exitCheckboxMode();
        loadMasjids();
    }

    @Override
    public void onPause() {
        super.onPause();
        exitCheckboxMode();
    }

    @Override
    public void onDestroyView() {
        UiDataModel.getUiDataModel().removeTabListener(mTabListener);
        super.onDestroyView();
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (!menuVisible) {
            exitCheckboxMode();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            exitCheckboxMode();
        }
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
        final Context appContext = context.getApplicationContext();

        boolean filled = masjid.fillMissingValuesWithDefaults(appContext);
        if (filled) {
            Toast.makeText(requireContext(), R.string.masjid_missing_values_defaults_toast, Toast.LENGTH_LONG).show();
            AppExecutors.getDiskIO().execute(() -> {
                SalahDatabase.getDatabase(appContext).masjidDao().update(masjid);
            });
        }

        mMasjidTitleText.setText(masjid.name);

        View rootView = getView();
        if (rootView != null) {
            ImageView masjidTitleIcon = rootView.findViewById(R.id.masjid_title_icon);
            if (masjidTitleIcon != null) {
                if (masjid.iconUri != null && !masjid.iconUri.isEmpty()) {
                    masjidTitleIcon.setImageURI(Uri.parse(masjid.iconUri));
                    masjidTitleIcon.setImageTintList(null);
                } else {
                    masjidTitleIcon.setImageResource(R.drawable.ic_masjid_timings);
                    masjidTitleIcon.setImageTintList(ColorStateList.valueOf(
                            MaterialColors.getColor(masjidTitleIcon, androidx.appcompat.R.attr.colorPrimary)));
                }
            }
        }

        mMasjidTitleClickableArea.setOnClickListener(v -> {
            exitCheckboxMode();
            MasjidEditDialogFragment.show(getParentFragmentManager(), masjid.id);
        });

        if (mViewTimesheetButton != null) {
            mViewTimesheetButton.setOnClickListener(v -> {
                exitCheckboxMode();
                Intent intent = new Intent(requireContext(), MasjidCardEditorActivity.class);
                intent.putExtra("masjid_id", masjid.id);
                startActivity(intent);
            });
        }

        if (mMasjids.size() > 1) {
            mPrevMasjidButton.setVisibility(View.VISIBLE);
            mNextMasjidButton.setVisibility(View.VISIBLE);
        } else {
            mPrevMasjidButton.setVisibility(View.GONE);
            mNextMasjidButton.setVisibility(View.GONE);
        }

        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            List<PrayerItem> items = new ArrayList<>();
            String[] types = {"FAJR", "DHUHR", "JUMA", "ASR", "MAGHRIB", "ISHA"};

            double lat = masjid.latitude;
            double lon = masjid.longitude;
            CalculationMethod method = masjid.getCalculationMethod(appContext);
            Madhab madhabEnum = masjid.getMadhab(appContext);
            String madhab = madhabEnum != null ? madhabEnum.name() : "SHAFI";

            SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), lat, lon, method);

            for (String type : types) {
                MasjidEvent event = db.masjidEventDao().getMasjidEvent(masjid.id, type);
                long timeMillis = 0;
                String summary = "";
                boolean isConfigured = false;

                if (event != null && st != null) {
                    try {
                        timeMillis = SalahCalculator.evaluateMasjidEvent(appContext, event, st, new Date(), true);
                        if (timeMillis > 0 && event.definition != null && event.definition.anchor != null) {
                            isConfigured = true;
                            String anchorName = RelationalRule.getAnchorDescription(appContext, event.definition.anchor);
                            summary = event.definition.getSummary(appContext, anchorName, true);
                        }
                    } catch (Exception ignored) {
                        timeMillis = 0;
                        isConfigured = false;
                    }
                }

                if (!isConfigured && st != null) {
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

                items.add(new PrayerItem(type, SalahUtils.getLocalizedSalahName(appContext, type), timeMillis, summary, isConfigured, masjid, event));
            }

            AppExecutors.getMainThread().post(() -> {
                if (isAdded()) {
                    mAdapter.setItems(items);
                }
            });
        });
    }

    private void exitCheckboxMode() {
        if (mShowCheckboxes) {
            mShowCheckboxes = false;
            if (mAdapter != null) {
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
            }
        }
    }

    private void shiftMasjid(int delta) {
        exitCheckboxMode();
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
        exitCheckboxMode();
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
                .setTitle(R.string.masjids_category_title)
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
        exitCheckboxMode();
        MasjidEditDialogFragment.show(getParentFragmentManager(), 0);
    }

    private static class PrayerItem {
        String type;
        String name;
        long timeMillis;
        String summary;
        boolean isConfigured;
        Masjid masjid;
        MasjidEvent event;

        PrayerItem(String type, String name, long timeMillis, String summary, boolean isConfigured, Masjid masjid, MasjidEvent event) {
            this.type = type;
            this.name = name;
            this.timeMillis = timeMillis;
            this.summary = summary;
            this.isConfigured = isConfigured;
            this.masjid = masjid;
            this.event = event;
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
                    boolean eventSelectedSame = (oldItem.event == null && newItem.event == null) ||
                            (oldItem.event != null && newItem.event != null && oldItem.event.isSelected == newItem.event.isSelected);
                    return oldItem.type.equals(newItem.type) &&
                            oldItem.timeMillis == newItem.timeMillis &&
                            Objects.equals(oldItem.summary, newItem.summary) &&
                            oldItem.isConfigured == newItem.isConfigured &&
                            eventSelectedSame;
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

            if (mShowCheckboxes && item.isConfigured && item.event != null) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(item.event.isSelected);
            } else {
                holder.checkBox.setVisibility(View.GONE);
            }

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
                if (mShowCheckboxes) {
                    if (item.isConfigured && item.event != null) {
                        toggleEventSelection(holder.getBindingAdapterPosition());
                    } else {
                        exitCheckboxMode();
                    }
                } else {
                    if (item.masjid == null) return;
                    if ("DHUHR".equals(item.type) || "JUMA".equals(item.type)) {
                        showFixedTimeDialogue(item.masjid, item.type);
                    } else {
                        SalahMasjidAlarmDialogFragment.show(getParentFragmentManager(), item.masjid.id, item.type);
                    }
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (item.isConfigured && item.event != null) {
                    mShowCheckboxes = !mShowCheckboxes;
                    notifyItemRangeChanged(0, getItemCount());
                    return true;
                }
                return false;
            });
        }

        private void toggleEventSelection(int position) {
            if (position < 0 || position >= mItems.size()) return;
            PrayerItem item = mItems.get(position);
            if (item == null || item.event == null) return;
            Context context = getContext();
            if (context == null) return;
            final Context appContext = context.getApplicationContext();

            item.event.isSelected = !item.event.isSelected;
            item.event.isManuallyDeselected = !item.event.isSelected;

            notifyItemChanged(position);

            final MasjidEvent eventToUpdate = item.event;
            AppExecutors.getDiskIO().execute(() -> {
                SalahDatabase.getDatabase(appContext).masjidEventDao().update(eventToUpdate);
                SalahAlarmSyncManager.syncSelectedRulesToAlarms(appContext);
            });
        }

        PrayerItem getItem(int position) {
            if (position >= 0 && position < mItems.size()) {
                return mItems.get(position);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            TextView name;
            TextView summary;
            TextView time;

            ViewHolder(View view) {
                super(view);
                checkBox = view.findViewById(R.id.prayer_checkbox);
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

                double[] coords = (masjid.latitude != 0.0 || masjid.longitude != 0.0)
                        ? new double[]{masjid.latitude, masjid.longitude}
                        : SalahUtils.getCoordinates(appContext);
                SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], masjid.getCalculationMethod(appContext));
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
                double[] coords = (masjid.latitude != 0.0 || masjid.longitude != 0.0)
                        ? new double[]{masjid.latitude, masjid.longitude}
                        : SalahUtils.getCoordinates(appContext);
                SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), coords[0], coords[1], masjid.getCalculationMethod(appContext));

                long evaluated = SalahCalculator.evaluateMasjidEvent(appContext, event, st, new Date(), true);
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
