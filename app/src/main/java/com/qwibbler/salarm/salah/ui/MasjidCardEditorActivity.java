package com.qwibbler.salarm.salah.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.qwibbler.salarm.R;
import com.qwibbler.salarm.base.AppExecutors;
import com.qwibbler.salarm.salah.Masjid;
import com.qwibbler.salarm.salah.MasjidCardData;
import com.qwibbler.salarm.salah.MasjidCardExporter;
import com.qwibbler.salarm.salah.MasjidCardRenderer;
import com.qwibbler.salarm.salah.SalahDatabase;
import com.qwibbler.salarm.uicomponents.CollapsingToolbarBaseActivity;

public class MasjidCardEditorActivity extends CollapsingToolbarBaseActivity {

    private Masjid masjid;
    private MasjidCardData cardData;
    private MasjidCardRenderer renderer;

    private MasjidCardView cardPreview;
    private FloatingActionButton btnFlipCard;
    private TextInputEditText editName;
    private TextInputEditText editSubtitle;
    private TextInputEditText editFooter;
    private TextInputLayout inputLayoutFooter;
    private MaterialButtonToggleGroup toggleFormat;

    private String currentExportType;
    private ActivityResultLauncher<Intent> createDocumentLauncher;

    @Override
    protected String getActivityTitle() {
        return getString(R.string.masjid_card_editor_title);
    }

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

        cardPreview = findViewById(R.id.card_preview);
        btnFlipCard = findViewById(R.id.btn_flip_card);
        editName = findViewById(R.id.edit_masjid_name);
        editSubtitle = findViewById(R.id.edit_masjid_subtitle);
        editFooter = findViewById(R.id.edit_footer_text);
        inputLayoutFooter = findViewById(R.id.input_layout_footer);
        toggleFormat = findViewById(R.id.toggle_format);

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
                cardData.format = MasjidCardData.PaperFormat.CARD;
                cardData.isFront = true;
                renderer.populatePrayerTimes(cardData, masjid, true);

                AppExecutors.getMainThread().post(() -> {
                    editName.setText(cardData.masjidName);
                    editSubtitle.setText(cardData.subtitle);
                    editFooter.setText(cardData.getFooterText());
                    toggleFormat.check(R.id.btn_format_card);
                    updateFloatingButtonAndFooterLabel();
                    cardPreview.setData(cardData);
                });
            }
        });
    }

    private void updateFloatingButtonAndFooterLabel() {
        if (cardData == null) return;
        btnFlipCard.setVisibility(View.VISIBLE);
        if (cardData.format == MasjidCardData.PaperFormat.CARD) {
            btnFlipCard.setImageResource(R.drawable.ic_turn);
            btnFlipCard.setContentDescription(getString(R.string.masjid_card_flip));
            btnFlipCard.setRotation(0f);
            if (cardData.isFront) {
                btnFlipCard.setScaleY(-1f);
                inputLayoutFooter.setHint(getString(R.string.masjid_card_footer_label_front));
            } else {
                btnFlipCard.setScaleY(1f);
                inputLayoutFooter.setHint(getString(R.string.masjid_card_footer_label_back));
            }
        } else {
            btnFlipCard.setImageResource(R.drawable.ic_orientation);
            btnFlipCard.setContentDescription(getString(R.string.masjid_card_orientation_toggle));
            btnFlipCard.setScaleX(1f);
            if (cardData.format == MasjidCardData.PaperFormat.A4_PORTRAIT) {
                btnFlipCard.setRotation(-90f);
            } else {
                btnFlipCard.setRotation(0f);
            }
            inputLayoutFooter.setHint(getString(R.string.masjid_card_footer_label));
        }
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

        editSubtitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (cardData != null) {
                    cardData.subtitle = s.toString();
                    cardPreview.invalidate();
                }
            }
        });

        editFooter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (cardData != null) {
                    if (cardData.format == MasjidCardData.PaperFormat.CARD && !cardData.isFront) {
                        cardData.footerTextBack = s.toString();
                    } else {
                        cardData.footerTextFront = s.toString();
                    }
                    cardPreview.invalidate();
                }
            }
        });

        btnFlipCard.setOnClickListener(v -> {
            if (masjid == null || cardData == null) return;
            if (cardData.format == MasjidCardData.PaperFormat.CARD) {
                boolean newFront = !cardData.isFront;
                AppExecutors.getDiskIO().execute(() -> {
                    renderer.populatePrayerTimes(cardData, masjid, newFront);
                    AppExecutors.getMainThread().post(() -> {
                        editFooter.setText(cardData.getFooterText());
                        updateFloatingButtonAndFooterLabel();
                        cardPreview.setData(cardData);
                    });
                });
            } else {
                boolean isCurrentlyPortrait = (cardData.format == MasjidCardData.PaperFormat.A4_PORTRAIT);
                cardData.format = isCurrentlyPortrait ? MasjidCardData.PaperFormat.A4_LANDSCAPE : MasjidCardData.PaperFormat.A4_PORTRAIT;
                updateFloatingButtonAndFooterLabel();
                cardPreview.setData(cardData);
            }
        });

        toggleFormat.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && masjid != null && cardData != null) {
                if (checkedId == R.id.btn_format_card) {
                    cardData.format = MasjidCardData.PaperFormat.CARD;
                    AppExecutors.getDiskIO().execute(() -> {
                        renderer.populatePrayerTimes(cardData, masjid, cardData.isFront);
                        AppExecutors.getMainThread().post(() -> {
                            editFooter.setText(cardData.getFooterText());
                            updateFloatingButtonAndFooterLabel();
                            cardPreview.setData(cardData);
                        });
                    });
                } else if (checkedId == R.id.btn_format_a4) {
                    cardData.format = MasjidCardData.PaperFormat.A4_PORTRAIT;
                    AppExecutors.getDiskIO().execute(() -> {
                        renderer.populatePrayerTimes(cardData, masjid);
                        AppExecutors.getMainThread().post(() -> {
                            editFooter.setText(cardData.getFooterText());
                            updateFloatingButtonAndFooterLabel();
                            cardPreview.setData(cardData);
                        });
                    });
                }
            }
        });

        findViewById(R.id.btn_export_png).setOnClickListener(v -> export("image/png", ".png"));
        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> export("application/pdf", ".pdf"));
        findViewById(R.id.btn_export_svg).setOnClickListener(v -> export("image/svg+xml", ".svg"));
    }

    private void export(String mimeType, String extension) {
        if (masjid == null || cardData == null) return;
        currentExportType = mimeType;
        String formatSuffix;
        if (cardData.format == MasjidCardData.PaperFormat.CARD) {
            formatSuffix = "_card";
        } else if (cardData.format == MasjidCardData.PaperFormat.A4_PORTRAIT) {
            formatSuffix = "_a4_portrait";
        } else {
            formatSuffix = "_a4_landscape";
        }
        String fileName = masjid.name.replaceAll("[^a-zA-Z0-9]", "_") + formatSuffix + extension;

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
