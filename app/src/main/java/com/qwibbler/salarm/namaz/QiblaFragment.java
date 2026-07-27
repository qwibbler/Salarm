package com.qwibbler.salarm.namaz;

import static com.qwibbler.salarm.uidata.UiDataModel.Tab.QIBLA;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Qibla;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.SalarmFragment;
import com.qwibbler.salarm.utils.Utils;

public class QiblaFragment extends SalarmFragment implements SensorEventListener {

    private ImageView mCompassBackground;
    private ImageView mQiblaNeedle;
    private TextView mStatusText;
    private Button mSetLocationButton;

    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;

    private double mQiblaDegree = -1;

    public QiblaFragment() {
        super(QIBLA);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.qibla_fragment, container, false);
        mCompassBackground = view.findViewById(R.id.compass_background);
        mQiblaNeedle = view.findViewById(R.id.qibla_needle);
        mStatusText = view.findViewById(R.id.qibla_status);
        mSetLocationButton = view.findViewById(R.id.btn_set_location);

        mSetLocationButton.setOnClickListener(v -> Utils.showDialogFragment(getParentFragmentManager(), new NamazSettingsDialog(), "namaz_settings"));

        mSensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if (mRotationVectorSensor == null) {
            mStatusText.setText(R.string.qibla_sensors_unavailable);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRotationVectorSensor != null) {
            mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
        updateQiblaDirection();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRotationVectorSensor != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    private void updateQiblaDirection() {
        Context context = requireContext();
        double[] coords = NamazUtils.getCoordinates(context);
        if (NamazLocationManager.isLocationSet(context)) {
            Coordinates coordinates = new Coordinates(coords[0], coords[1]);
            mQiblaDegree = new Qibla(coordinates).direction;
            mStatusText.setText(R.string.qibla_calibration_tip);
            mSetLocationButton.setVisibility(View.GONE);
            mCompassBackground.setAlpha(1.0f);
        } else {
            mQiblaDegree = -1;
            mStatusText.setText(R.string.qibla_location_not_set);
            mSetLocationButton.setVisibility(View.VISIBLE);
            mCompassBackground.setAlpha(0.3f);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            float azimuth = (float) Math.toDegrees(orientation[0]);
            rotateCompass(azimuth);
        }
    }

    private void rotateCompass(float azimuth) {
        float degree = -azimuth;

        // Smooth rotation for background
        float currentRotation = mCompassBackground.getRotation();
        float delta = degree - (currentRotation % 360);
        if (delta > 180) delta -= 360;
        else if (delta < -180) delta += 360;
        mCompassBackground.animate().rotation(currentRotation + delta).setDuration(150).start();

        if (mQiblaDegree != -1) {
            mQiblaNeedle.setVisibility(View.VISIBLE);
            float needleTarget = (float) (degree + mQiblaDegree);
            float currentNeedleRotation = mQiblaNeedle.getRotation();
            float needleDelta = needleTarget - (currentNeedleRotation % 360);
            if (needleDelta > 180) needleDelta -= 360;
            else if (needleDelta < -180) needleDelta += 360;
            mQiblaNeedle.animate().rotation(currentNeedleRotation + needleDelta).setDuration(150).start();
        } else {
            mQiblaNeedle.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
