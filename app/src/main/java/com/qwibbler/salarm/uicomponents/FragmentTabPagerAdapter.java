/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.qwibbler.salarm.uicomponents;

import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;

import com.qwibbler.salarm.SalarmActivity;
import com.qwibbler.salarm.base.SalarmFragment;
import com.qwibbler.salarm.uidata.UiDataModel;

import java.util.Map;

/**
 * This adapter produces the SalarmFragments that are the content of the Salarm tabs. The
 * adapter presents the tabs in LTR and RTL order depending on the text layout direction for the
 * current locale. To prevent issues when switching between LTR and RTL, fragments are registered
 * with the manager using position-independent tags, which is an important departure from
 * FragmentPagerAdapter.
 */
public final class FragmentTabPagerAdapter extends PagerAdapter {

    private final SalarmActivity mSalarmActivity;

    /**
     * The manager into which fragments are added.
     */
    private final FragmentManager mFragmentManager;

    /**
     * A fragment cache that can be accessed before {@link #instantiateItem} is called.
     */
    private final Map<UiDataModel.Tab, SalarmFragment> mFragmentCache;

    /**
     * The active fragment transaction if one exists.
     */
    private FragmentTransaction mCurrentTransaction;

    /**
     * The current fragment displayed to the user.
     */
    private Fragment mCurrentPrimaryItem;

    public FragmentTabPagerAdapter(SalarmActivity salarmActivity) {
        mSalarmActivity = salarmActivity;
        mFragmentCache = new ArrayMap<>(getCount());
        mFragmentManager = salarmActivity.getSupportFragmentManager();
    }

    @Override
    public int getCount() {
        return UiDataModel.getUiDataModel().getTabCount();
    }

    /**
     * @param position the left-to-right index of the fragment to be returned
     * @return the fragment displayed at the given {@code position}
     */
    public SalarmFragment getSalarmFragment(int position) {
        // Fetch the tab the UiDataModel reports for the position.
        final UiDataModel.Tab tab = UiDataModel.getUiDataModel().getTabAt(position);

        // First check the local cache for the fragment.
        SalarmFragment fragment = mFragmentCache.get(tab);
        if (fragment != null) {
            return fragment;
        }

        // Next check the fragment manager; relevant when app is rebuilt after locale changes
        // because this adapter will be new and mFragmentCache will be empty, but the fragment
        // manager will retain the Fragments built on original application launch.
        fragment = (SalarmFragment) mFragmentManager.findFragmentByTag(tab.name());
        if (fragment != null) {
            fragment.setFabContainer(mSalarmActivity);
            mFragmentCache.put(tab, fragment);
            return fragment;
        }

        // Otherwise, build the fragment from scratch.
        final String fragmentClassName = tab.getFragmentClassName();
        FragmentFactory fragmentFactory = mFragmentManager.getFragmentFactory();
        fragment = (SalarmFragment) fragmentFactory.instantiate(mSalarmActivity.getClassLoader(), fragmentClassName);
        fragment.setFabContainer(mSalarmActivity);
        mFragmentCache.put(tab, fragment);
        return fragment;
    }

    @Override
    public void startUpdate(ViewGroup container) {
        if (container.getId() == View.NO_ID) {
            throw new IllegalStateException("ViewPager with adapter " + this + " has no id");
        }
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        if (mCurrentTransaction == null) {
            mCurrentTransaction = mFragmentManager.beginTransaction();
        }

        // Use the fragment located in the fragment manager if one exists.
        final UiDataModel.Tab tab = UiDataModel.getUiDataModel().getTabAt(position);
        Fragment fragment = mFragmentManager.findFragmentByTag(tab.name());
        if (fragment != null) {
            mCurrentTransaction.attach(fragment);
        } else {
            fragment = getSalarmFragment(position);
            mCurrentTransaction.add(container.getId(), fragment, tab.name());
        }

        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
        }

        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (mCurrentTransaction == null) {
            mCurrentTransaction = mFragmentManager.beginTransaction();
        }
        final SalarmFragment fragment = (SalarmFragment) object;
        fragment.setFabContainer(null);
        mCurrentTransaction.detach(fragment);
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        final Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
            }

            fragment.setMenuVisibility(true);
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(@NonNull ViewGroup container) {
        if (mCurrentTransaction != null) {
            mCurrentTransaction.commitAllowingStateLoss();
            mCurrentTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return ((Fragment) object).getView() == view;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        // Since we only call `notifyDataSetChanged()` when a major change occurs (such as adding or removing tabs),
        // we instruct the ViewPager to detach and re-evaluate all its active fragments.
        return POSITION_NONE;
    }

    public void clearCache() {
        mFragmentCache.clear();
    }

}
