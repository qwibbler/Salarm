package com.qwibbler.salarm.salah;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.uicomponents.CustomDialog;
import com.qwibbler.salarm.utils.LogUtils;
import com.qwibbler.salarm.utils.Utils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasjidQrImportDialogFragment extends DialogFragment {

    private PreviewView mPreviewView;
    private View mCameraPermissionContainer;
    private View mDetectedPromptContainer;
    private TextView mTvDetectedTitle;
    private MaterialButton mBtnRescan;
    private TextInputEditText mInputEditText;

    private ProcessCameraProvider mCameraProvider;
    private ExecutorService mCameraExecutor;
    private ActivityResultLauncher<String> mPermissionLauncher;
    private volatile boolean mIsScanning = false;

    public static void show(FragmentManager manager) {
        MasjidQrImportDialogFragment fragment = new MasjidQrImportDialogFragment();
        Utils.showDialogFragment(manager, fragment, "MasjidQrImportDialogFragment");
    }

    private static byte[] rotateYUV(byte[] yuv, int width, int height, int rotation) {
        if (rotation == 0) return yuv;
        byte[] rotated = new byte[yuv.length];
        if (rotation == 90) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rotated[x * height + (height - 1 - y)] = yuv[y * width + x];
                }
            }
        } else if (rotation == 270) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rotated[(width - 1 - x) * height + y] = yuv[y * width + x];
                }
            }
        } else if (rotation == 180) {
            for (int i = 0; i < width * height; i++) {
                rotated[width * height - 1 - i] = yuv[i];
            }
        } else {
            return yuv;
        }
        return rotated;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameraExecutor = Executors.newSingleThreadExecutor();
        mPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    showCameraUI();
                    startCamera();
                } else {
                    showPermissionDeniedUI();
                }
            }
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        View view = getLayoutInflater().inflate(R.layout.dialog_masjid_qr_import, null);

        mPreviewView = view.findViewById(R.id.camera_preview);
        mCameraPermissionContainer = view.findViewById(R.id.camera_permission_container);
        mDetectedPromptContainer = view.findViewById(R.id.detected_prompt_container);
        mTvDetectedTitle = view.findViewById(R.id.tv_detected_title);
        mBtnRescan = view.findViewById(R.id.btn_rescan);
        mInputEditText = view.findViewById(R.id.qr_input_edit_text);

        View btnGrantPermission = view.findViewById(R.id.btn_grant_camera_permission);
        if (btnGrantPermission != null) {
            btnGrantPermission.setOnClickListener(v -> requestCameraPermission());
        }

        if (mBtnRescan != null) {
            mBtnRescan.setOnClickListener(v -> rescan());
        }

        View btnPaste = view.findViewById(R.id.btn_paste_clipboard);
        if (btnPaste != null) {
            btnPaste.setOnClickListener(v -> pasteFromClipboard());
        }

        if (hasCameraPermission()) {
            showCameraUI();
            startCamera();
        } else {
            showPermissionDeniedUI();
            requestCameraPermission();
        }

        // Auto check clipboard on open if camera hasn't scanned yet
        checkClipboardAutoFill();

        return CustomDialog.create(
            context,
            null,
            null,
            getString(R.string.masjid_import_qr_title),
            null,
            view,
            getString(R.string.masjid_import_qr_btn_import),
            (dialog, which) -> processImport(),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        mPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void showCameraUI() {
        if (mCameraPermissionContainer != null) {
            mCameraPermissionContainer.setVisibility(View.GONE);
        }
    }

    private void showPermissionDeniedUI() {
        if (mCameraPermissionContainer != null) {
            mCameraPermissionContainer.setVisibility(View.VISIBLE);
        }
    }

    private void startCamera() {
        if (!hasCameraPermission() || getContext() == null) return;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                LogUtils.e("Failed to initialize camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (mCameraProvider == null || !isAdded() || mPreviewView == null) return;

        mCameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        MultiFormatReader reader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        reader.setHints(hints);

        mIsScanning = true;
        imageAnalysis.setAnalyzer(mCameraExecutor, imageProxy -> {
            if (!mIsScanning) {
                imageProxy.close();
                return;
            }
            try {
                ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
                if (planes.length > 0) {
                    BinaryBitmap bitmap = getBinaryBitmap(imageProxy, planes);
                    Result result = reader.decodeWithState(bitmap);
                    if (result != null && result.getText() != null && !result.getText().trim().isEmpty()) {
                        mIsScanning = false;
                        String scannedText = result.getText().trim();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> onQrCodeDetected(scannedText));
                        }
                    }
                }
            } catch (Exception ignored) {
                // No QR code found in this frame
            } finally {
                reader.reset();
                imageProxy.close();
            }
        });

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        try {
            mCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            LogUtils.e("Failed to bind camera use cases", e);
        }
    }

    @NonNull
    private static BinaryBitmap getBinaryBitmap(ImageProxy imageProxy, ImageProxy.PlaneProxy[] planes) {
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        int rotation = imageProxy.getImageInfo().getRotationDegrees();

        byte[] rotated = rotateYUV(data, width, height, rotation);
        int finalWidth = (rotation == 90 || rotation == 270) ? height : width;
        int finalHeight = (rotation == 90 || rotation == 270) ? width : height;

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
            rotated, finalWidth, finalHeight, 0, 0, finalWidth, finalHeight, false
        );
        return new BinaryBitmap(new HybridBinarizer(source));
    }

    private void onQrCodeDetected(String scannedText) {
        if (!isAdded()) return;

        if (mInputEditText != null) {
            mInputEditText.setText(scannedText);
            Utils.performHapticFeedback(mInputEditText, HapticFeedbackConstantsCompat.CONFIRM);
        }

        if (mBtnRescan != null) {
            mBtnRescan.setVisibility(View.VISIBLE);
        }

        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(requireContext().getApplicationContext(), scannedText);
        if (mDetectedPromptContainer != null && mTvDetectedTitle != null) {
            mDetectedPromptContainer.setVisibility(View.VISIBLE);
            if (result != null && result.masjid != null && result.masjid.name != null) {
                mTvDetectedTitle.setText(result.masjid.name);
            } else {
                mTvDetectedTitle.setText(R.string.masjid_import_qr_title);
            }
        }
    }

    private void rescan() {
        if (mDetectedPromptContainer != null) {
            mDetectedPromptContainer.setVisibility(View.GONE);
        }
        if (mBtnRescan != null) {
            mBtnRescan.setVisibility(View.GONE);
        }
        mIsScanning = true;
    }

    private void checkClipboardAutoFill() {
        try {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clipData = clipboard.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    CharSequence clipText = clipData.getItemAt(0).getText();
                    if (clipText != null && clipText.toString().trim().startsWith("salarm:")) {
                        mInputEditText.setText(clipText.toString().trim());
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("Failed to check clipboard", e);
        }
    }

    private void pasteFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clipData = clipboard.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    CharSequence clipText = clipData.getItemAt(0).getText();
                    if (clipText != null) {
                        mInputEditText.setText(clipText.toString().trim());
                        return;
                    }
                }
            }
            Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            LogUtils.e("Failed to paste from clipboard", e);
        }
    }

    private void processImport() {
        if (mInputEditText == null || mInputEditText.getText() == null) return;
        String rawText = mInputEditText.getText().toString().trim();

        Context appContext = requireContext().getApplicationContext();
        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(appContext, rawText);

        if (result == null || result.masjid == null) {
            Toast.makeText(appContext, R.string.masjid_import_qr_invalid_toast, Toast.LENGTH_SHORT).show();
            return;
        }

        AppExecutors.getDiskIO().execute(() -> {
            SalahDatabase db = SalahDatabase.getDatabase(appContext);
            result.masjid.fillMissingValuesWithDefaults(appContext);

            long newMasjidId = db.masjidDao().insert(result.masjid);

            if (result.events != null) {
                for (MasjidEvent event : result.events) {
                    event.masjidId = newMasjidId;
                    db.masjidEventDao().insert(event);
                }
            }

            SalahAlarmSyncManager.syncSelectedRulesToAlarms(appContext);

            AppExecutors.getMainThread().post(() -> {
                if (getContext() != null) {
                    Toast.makeText(appContext, getString(R.string.masjid_import_qr_success_toast, result.masjid.name), Toast.LENGTH_SHORT).show();
                    Bundle resultBundle = new Bundle();
                    resultBundle.putLong("selected_masjid_id", newMasjidId);
                    getParentFragmentManager().setFragmentResult("masjid_updated", resultBundle);
                    getParentFragmentManager().setFragmentResult("masjid_imported", resultBundle);
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraExecutor != null) {
            mCameraExecutor.shutdown();
        }
    }
}
