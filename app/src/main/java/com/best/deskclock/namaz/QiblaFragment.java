package com.best.deskclock.namaz;

import static com.best.deskclock.uidata.UiDataModel.Tab.QIBLA;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.base.DeskClockFragment;

public class QiblaFragment extends DeskClockFragment {

    public QiblaFragment() {
        super(QIBLA);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.qibla_fragment, container, false);
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setVisibility(View.GONE);
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        left.setVisibility(View.GONE);
        right.setVisibility(View.GONE);
    }

    @Override
    public void onFabClick() {
    }
}
