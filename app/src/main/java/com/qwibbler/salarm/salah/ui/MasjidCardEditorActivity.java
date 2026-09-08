package com.qwibbler.salarm.salah.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.MasjidCardData;
import com.qwibbler.salarm.salah.MasjidCardExporter;
import com.qwibbler.salarm.salah.MasjidCardRenderer;
import com.qwibbler.salarm.salah.SalahDatabase;

public class MasjidCardEditorActivity extends AppCompatActivity {

    private Masjid masjid;
    private MasjidCardData cardData;
    private MasjidCardRenderer renderer;

    private MasjidCardView cardPreview;
    private TextInputEditText editName;
    private TextInputEditText editFooter;
    private MaterialButtonToggleGroup toggleSide;

    private String currentExportType;
    private ActivityResultLauncher<Intent> createDocumentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masjid_card_editor);

        createDocumentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    performExport(uri);
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        cardPreview = findViewById(R.id.card_preview);
        editName = findViewById(R.id.edit_masjid_name);
        editFooter = findViewById(R.id.edit_footer_text);
        toggleSide = findViewById(R.id.toggle_side);

        renderer = new MasjidCardRenderer(this);

        long masjidId = getIntent().getLongExtra("masjid_id", -1);
        if (masjidId != -1) {
            loadMasjid(masjidId);
        }

        setupListeners();
    }

    private void loadMasjid(long id) {
        AppExecutors.getDiskIO().execute(() -> {
            masjid = SalahDatabase.getDatabase(this).masjidDao().getById(id);
            if (masjid != null) {
                cardData = renderer.generateData(masjid);
                renderer.populatePrayerTimes(cardData, masjid, true);

                AppExecutors.getMainThread().post(() -> {
                    editName.setText(cardData.masjidName);
                    editFooter.setText(cardData.getFooterText());
                    toggleSide.check(R.id.btn_front);
                    cardPreview.setData(cardData);
                });
            }
        });
    }

    private void setupListeners() {
        editName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (cardData != null) {
                    cardData.masjidName = s.toString();
                    cardPreview.invalidate();
                }
            }
        });

        editFooter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (cardData != null) {
                    if (cardData.isFront) {
                        cardData.footerTextFront = s.toString();
                    } else {
                        cardData.footerTextBack = s.toString();
                    }
                    cardPreview.invalidate();
                }
            }
        });

        toggleSide.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && masjid != null && cardData != null) {
                boolean isFront = checkedId == R.id.btn_front;
                AppExecutors.getDiskIO().execute(() -> {
                    renderer.populatePrayerTimes(cardData, masjid, isFront);
                    AppExecutors.getMainThread().post(() -> {
                        editFooter.setText(cardData.getFooterText());
                        cardPreview.setData(cardData);
                    });
                });
            }
        });

        findViewById(R.id.btn_export_png).setOnClickListener(v -> export("image/png", ".png"));
        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> export("application/pdf", ".pdf"));
        findViewById(R.id.btn_export_svg).setOnClickListener(v -> export("image/svg+xml", ".svg"));
    }

    private void export(String mimeType, String extension) {
        if (masjid == null || cardData == null) return;
        currentExportType = mimeType;
        String fileName = masjid.name.replaceAll("[^a-zA-Z0-9]", "_") + "_card" + extension;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        createDocumentLauncher.launch(intent);
    }

    private void performExport(Uri uri) {
        if (masjid == null || cardData == null || currentExportType == null) return;

        MasjidCardExporter exporter = new MasjidCardExporter(this, renderer);

        AppExecutors.getDiskIO().execute(() -> {
            switch (currentExportType) {
                case "image/png":
                    exporter.exportPng(cardData, uri);
                    break;
                case "application/pdf":
                    exporter.exportPdf(masjid, cardData, uri);
                    break;
                case "image/svg+xml":
                    exporter.exportSvg(cardData, uri);
                    break;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
