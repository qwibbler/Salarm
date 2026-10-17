package com.qwibbler.salarm.salah;

import static com.qwibbler.salarm.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;
import static com.qwibbler.salarm.uidata.UiDataModel.Tab.MASJID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.batoulapps.adhan.CalculationMethod;
import com.google.android.material.color.MaterialColors;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.SalarmApplication;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.base.SalarmFragment;
import com.qwibbler.salarm.data.SettingsDAO;
import com.qwibbler.salarm.dialogfragment.MaterialTimePickerDialogFragment;
import com.qwibbler.salarm.dialogfragment.SpinnerTimePickerDialogFragment;
import com.qwibbler.salarm.salah.ui.MasjidCardEditorActivity;
import com.qwibbler.salarm.settings.MasjidEditDialogFragment;
import com.qwibbler.salarm.settings.SalahMasjidAlarmDialogFragment;
import com.qwibbler.salarm.uicomponents.CustomTooltip;
import com.qwibbler.salarm.uidata.TabListener;
import com.qwibbler.salarm.uidata.UiDataModel;
import com.qwibbler.salarm.utils.LogUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MasjidFragment extends SalarmFragment {

    private static final String KEY_SIDE_BUTTONS_VISIBLE = "key_side_buttons_visible";
    private View mEmptyStateLayout;
    private View mContentLayout;
    private View mPrevMasjidButton;
    private View mNextMasjidButton;
    private View mMasjidTitleClickableArea;
    private TextView mMasjidTitleText;
    private RecyclerView mRecyclerView;
    private MasjidPrayerAdapter mAdapter;
    private List<Masjid> mMasjids = new ArrayList<>();
    private long mSelectedMasjidId = -1;
    private boolean mShowCheckboxes = false;
    private boolean mSideButtonsVisible = false;

    private final TabListener mTabListener = newSelectedTab -> {
        if (newSelectedTab != MASJID) {
            exitCheckboxMode();
            hideSideButtonsOnlyAnimated();
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

        if (mEmptyStateLayout != null) {
            mEmptyStateLayout.setOnClickListener(v -> {
                if (mSideButtonsVisible) {
                    hideSideButtonsWithFabAnimation();
                } else {
                    showAddOrImportDialog();
                }
            });
        }

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new MasjidPrayerAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mPrevMasjidButton.setOnClickListener(v -> {
            hideSideButtonsWithFabAnimation();
            shiftMasjid(-1);
        });
        mPrevMasjidButton.setOnLongClickListener(v -> {
            showMasjidPickerDialog();
            return true;
        });

        mNextMasjidButton.setOnClickListener(v -> {
            hideSideButtonsWithFabAnimation();
            shiftMasjid(1);
        });
        mNextMasjidButton.setOnLongClickListener(v -> {
            showMasjidPickerDialog();
            return true;
        });

        view.setOnClickListener(v -> {
            exitCheckboxMode();
            hideSideButtonsWithFabAnimation();
        });
        mContentLayout.setOnClickListener(v -> {
            exitCheckboxMode();
            hideSideButtonsWithFabAnimation();
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideSideButtonsWithFabAnimation();
                }
            }
        });

        mRecyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    if (mSideButtonsVisible) {
                        hideSideButtonsWithFabAnimation();
                    }
                }
                return false;
            }
        });

        if (savedInstanceState != null) {
            mSelectedMasjidId = savedInstanceState.getLong("selected_masjid_id", -1);
            mSideButtonsVisible = savedInstanceState.getBoolean(KEY_SIDE_BUTTONS_VISIBLE, false);
        }

        UiDataModel.getUiDataModel().addTabListener(mTabListener);

        FragmentResultListener updateListener = (requestKey, result) -> {
            long selectedId = result.getLong("selected_masjid_id", -1);
            if (selectedId != -1) {
                mSelectedMasjidId = selectedId;
            }
            loadMasjids();
        };

        getParentFragmentManager().setFragmentResultListener("masjid_updated", this, updateListener);
        getParentFragmentManager().setFragmentResultListener("masjid_imported", this, updateListener);

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
        resetFabAndButtonsState();
    }

    @Override
    public void onDestroyView() {
        UiDataModel.getUiDataModel().removeTabListener(mTabListener);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("selected_masjid_id", mSelectedMasjidId);
        outState.putBoolean(KEY_SIDE_BUTTONS_VISIBLE, mSideButtonsVisible);
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (!menuVisible) {
            exitCheckboxMode();
            hideSideButtonsOnlyAnimated();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            exitCheckboxMode();
            hideSideButtonsOnlyAnimated();
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
            AppExecutors.getDiskIO().execute(() -> SalahDatabase.getDatabase(appContext).masjidDao().update(masjid));
        }

        mMasjidTitleText.setText(masjid.name);

        View rootView = getView();
        if (rootView != null) {
            ImageView masjidTitleIcon = rootView.findViewById(R.id.masjid_title_icon);
            if (masjidTitleIcon != null) {
                if (masjid.iconUri != null && !masjid.iconUri.isEmpty()) {
                    try {
                        masjidTitleIcon.setImageURI(Uri.parse(masjid.iconUri));
                        masjidTitleIcon.setImageTintList(null);
                    } catch (Exception e) {
                        LogUtils.e("Failed to load masjid icon URI", e);
                        masjidTitleIcon.setImageResource(R.drawable.ic_masjid_timings);
                        masjidTitleIcon.setImageTintList(ColorStateList.valueOf(
                            MaterialColors.getColor(masjidTitleIcon, androidx.appcompat.R.attr.colorPrimary)));
                    }
                } else {
                    masjidTitleIcon.setImageResource(R.drawable.ic_masjid_timings);
                    masjidTitleIcon.setImageTintList(ColorStateList.valueOf(
                        MaterialColors.getColor(masjidTitleIcon, androidx.appcompat.R.attr.colorPrimary)));
                }
            }
        }

        mMasjidTitleClickableArea.setOnClickListener(v -> {
            exitCheckboxMode();
            hideSideButtonsWithFabAnimation();
            MasjidEditDialogFragment.show(getParentFragmentManager(), masjid.id);
        });

        updateFab(FAB_AND_BUTTONS_IMMEDIATE);

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

            SalahCalculator.SalahTimes st = SalahCalculator.calculateAllSalahTimes(appContext, new Date(), lat, lon, method);

            for (String type : types) {
                MasjidEvent event = db.masjidEventDao().getMasjidEvent(masjid.id, type);
                long timeMillis = 0;
                String summary = "";
                boolean isConfigured = false;

                if (event != null && event.definition != null) {
                    try {
                        timeMillis = SalahCalculator.evaluateMasjidEvent(appContext, event, st, new Date(), true);
                        if (timeMillis > 0) {
                            isConfigured = true;
                            if (!"DHUHR".equals(type) && !"JUMA".equals(type) && event.definition.anchor != null) {
                                String anchorName = RelationalRule.getAnchorDescription(appContext, event.definition.anchor);
                                summary = event.definition.getSummary(appContext, anchorName, true);
                            }
                        }
                    } catch (Exception ignored) {
                        timeMillis = 0;
                        isConfigured = false;
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

    private void hideSideButtonsWithFabAnimation() {
        if (mSideButtonsVisible) {
            mSideButtonsVisible = false;
            updateFab(FAB_AND_BUTTONS_SHRINK_AND_EXPAND);
        }
    }

    public void hideSideButtonsOnlyAnimated() {
        if (mSideButtonsVisible) {
            mSideButtonsVisible = false;
            updateFab(BUTTONS_SHRINK_AND_EXPAND);
        }
    }

    public void resetFabAndButtonsState() {
        if (mSideButtonsVisible) {
            mSideButtonsVisible = false;
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
        }
    }

    private void showAddOrImportDialog() {
        exitCheckboxMode();
        CharSequence[] options = new CharSequence[]{
            getString(R.string.salarm_add_new_masjid),
            getString(R.string.masjid_import_qr_title)
        };
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.masjids_category_title)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    MasjidEditDialogFragment.show(getParentFragmentManager(), 0);
                } else if (which == 1) {
                    MasjidQrImportDialogFragment.show(getParentFragmentManager());
                }
            })
            .show();
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setContentDescription(getString(R.string.salarm_add_new_masjid));
        if (mSideButtonsVisible) {
            fab.setVisibility(View.INVISIBLE);
        } else {
            fab.setImageResource(R.drawable.ic_add);
            fab.setVisibility(View.VISIBLE);
            fab.setOnLongClickListener(v -> {
                CustomTooltip.showAbove(v, fab.getContentDescription().toString(), true);
                return true;
            });
        }
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        if (mSideButtonsVisible) {
            left.setVisibility(View.VISIBLE);
            right.setVisibility(View.VISIBLE);

            left.setClickable(true);
            left.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_add_masjid));
            left.setContentDescription(getString(R.string.salarm_add_new_masjid));
            left.setOnClickListener(v -> {
                hideSideButtonsWithFabAnimation();
                MasjidEditDialogFragment.show(getParentFragmentManager(), 0);
            });
            left.setOnLongClickListener(v -> {
                CustomTooltip.showAbove(v, getString(R.string.salarm_add_new_masjid), true);
                return true;
            });

            right.setClickable(true);
            right.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_qr_code));
            right.setContentDescription(getString(R.string.masjid_import_qr_title));
            right.setOnClickListener(v -> {
                hideSideButtonsWithFabAnimation();
                MasjidQrImportDialogFragment.show(getParentFragmentManager());
            });
            right.setOnLongClickListener(v -> {
                CustomTooltip.showAbove(v, getString(R.string.masjid_import_qr_title), true);
                return true;
            });
        } else {
            left.setVisibility(View.INVISIBLE);
            right.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onFabClick() {
        exitCheckboxMode();
        if (mSideButtonsVisible) {
            hideSideButtonsWithFabAnimation();
        } else {
            mSideButtonsVisible = true;
            updateFab(FAB_AND_BUTTONS_SHRINK_AND_EXPAND);
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

    private class MasjidPrayerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_PRAYER = 0;
        private static final int VIEW_TYPE_TIMESHEET = 1;

        private List<PrayerItem> mItems = new ArrayList<>();

        void setItems(final List<PrayerItem> items) {
            final List<PrayerItem> oldItems = mItems;
            mItems = items;
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldItems.size() + (oldItems.isEmpty() ? 0 : 1);
                }

                @Override
                public int getNewListSize() {
                    return mItems.size() + (mItems.isEmpty() ? 0 : 1);
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    boolean oldIsTimesheet = oldItemPosition >= oldItems.size();
                    boolean newIsTimesheet = newItemPosition >= mItems.size();
                    if (oldIsTimesheet && newIsTimesheet) return true;
                    if (oldIsTimesheet || newIsTimesheet) return false;
                    return oldItems.get(oldItemPosition).type.equals(mItems.get(newItemPosition).type);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    boolean oldIsTimesheet = oldItemPosition >= oldItems.size();
                    boolean newIsTimesheet = newItemPosition >= mItems.size();
                    if (oldIsTimesheet && newIsTimesheet) return true;
                    if (oldIsTimesheet || newIsTimesheet) return false;
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

        @Override
        public int getItemViewType(int position) {
            if (position < mItems.size()) {
                return VIEW_TYPE_PRAYER;
            }
            return VIEW_TYPE_TIMESHEET;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_TIMESHEET) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.masjid_timesheet_button_item, parent, false);
                return new TimesheetViewHolder(view);
            }
            return new PrayerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.masjid_prayer_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == VIEW_TYPE_TIMESHEET) {
                TimesheetViewHolder timesheetHolder = (TimesheetViewHolder) holder;
                timesheetHolder.button.setOnClickListener(v -> {
                    exitCheckboxMode();
                    hideSideButtonsWithFabAnimation();
                    if (mSelectedMasjidId != -1) {
                        Intent intent = new Intent(requireContext(), MasjidCardEditorActivity.class);
                        intent.putExtra("masjid_id", mSelectedMasjidId);
                        startActivity(intent);
                    }
                });
                return;
            }

            PrayerViewHolder prayerHolder = (PrayerViewHolder) holder;
            PrayerItem item = mItems.get(position);
            prayerHolder.name.setText(item.name);

            if (mShowCheckboxes && item.isConfigured && item.event != null) {
                prayerHolder.checkBox.setVisibility(View.VISIBLE);
                prayerHolder.checkBox.setChecked(item.event.isSelected);
            } else {
                prayerHolder.checkBox.setVisibility(View.GONE);
            }

            if (item.isConfigured && item.summary != null && !item.summary.isEmpty()) {
                prayerHolder.summary.setText(item.summary);
                prayerHolder.summary.setVisibility(View.VISIBLE);
                prayerHolder.time.setTextColor(MaterialColors.getColor(prayerHolder.itemView, android.R.attr.textColorPrimary));
            } else {
                prayerHolder.summary.setVisibility(View.GONE);
                int secondaryColor = MaterialColors.getColor(prayerHolder.itemView, android.R.attr.textColorSecondary, Color.GRAY);
                prayerHolder.time.setTextColor(secondaryColor);
            }

            if (item.timeMillis > 0 && getContext() != null) {
                Date date = new Date(item.timeMillis);
                DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getContext());
                if (timeFormat != null) {
                    prayerHolder.time.setText(timeFormat.format(date));
                } else {
                    prayerHolder.time.setText("--:--");
                }
            } else {
                prayerHolder.time.setText("--:--");
            }

            prayerHolder.itemView.setOnClickListener(v -> {
                hideSideButtonsWithFabAnimation();
                if (mShowCheckboxes) {
                    if (item.isConfigured && item.event != null) {
                        toggleEventSelection(prayerHolder.getBindingAdapterPosition());
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

            prayerHolder.itemView.setOnLongClickListener(v -> {
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
            return mItems.size() + (mItems.isEmpty() ? 0 : 1);
        }

        class TimesheetViewHolder extends RecyclerView.ViewHolder {
            View button;

            TimesheetViewHolder(View view) {
                super(view);
                button = view.findViewById(R.id.view_timesheet_button);
            }
        }

        class PrayerViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            TextView name;
            TextView summary;
            TextView time;

            PrayerViewHolder(View view) {
                super(view);
                checkBox = view.findViewById(R.id.prayer_checkbox);
                name = view.findViewById(R.id.prayer_name);
                summary = view.findViewById(R.id.prayer_calc_summary);
                time = view.findViewById(R.id.prayer_time);
            }
        }
    }
}
