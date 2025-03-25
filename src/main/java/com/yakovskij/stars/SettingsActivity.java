package com.yakovskij.stars;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity {

    private EditText etMinMag, etLat, etLon, etTime;
    private Button btnSave, btnPickDate;
    private SwitchCompat switchDrawConstellations, switchDrawLabels, switchDrawHorizon, switchDrawStars, switchDrawGrid;
    private String selectedDate = "2020-01-01"; // Дата по умолчанию

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Настраиваем Toolbar и кнопку "Назад"
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // При нажатии на навигационную кнопку сохраняем данные и выходим
        toolbar.setNavigationOnClickListener(v -> {
            saveAndFinish();
        });

        etMinMag = findViewById(R.id.etMinMag);
        etLat = findViewById(R.id.etLat);
        etLon = findViewById(R.id.etLon);
        etTime = findViewById(R.id.etTime);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnSave = findViewById(R.id.btnSave);

        switchDrawConstellations = findViewById(R.id.switchDrawConstellations);
        switchDrawLabels = findViewById(R.id.switchDrawLabels);
        switchDrawGrid = findViewById(R.id.switchDrawGrid);
        switchDrawHorizon = findViewById(R.id.switchDrawHorizon);
        switchDrawStars = findViewById(R.id.switchDrawStars);

        // Получаем переданные данные из MainActivity
        Intent intent = getIntent();
        double minMag = intent.getDoubleExtra("minMag", 1.0);
        double lat = intent.getDoubleExtra("lat", 30.0);
        double lon = intent.getDoubleExtra("lon", 60.0);
        String time = intent.getStringExtra("time");

        // Получаем дополнительные настройки (если они переданы)
        boolean drawConstellations = intent.getBooleanExtra("drawConstellations", true);
        boolean drawLabels = intent.getBooleanExtra("drawLabels", true);
        boolean drawGrid = intent.getBooleanExtra("drawGrid", true);
        boolean drawHorizon = intent.getBooleanExtra("drawHorizon", true);
        boolean drawStars = intent.getBooleanExtra("drawStars", true);

        // Устанавливаем значения в EditText и переключатели
        etMinMag.setText(String.valueOf(minMag));
        etLat.setText(String.valueOf(lat));
        etLon.setText(String.valueOf(lon));
        etTime.setText(time);
        selectedDate = time; // сохраняем текущую дату

        switchDrawConstellations.setChecked(drawConstellations);
        switchDrawLabels.setChecked(drawLabels);
        switchDrawGrid.setChecked(drawGrid);
        switchDrawHorizon.setChecked(drawHorizon);
        switchDrawStars.setChecked(drawStars);

        // Обработчик выбора даты
        btnPickDate.setOnClickListener(v -> showDatePickerDialog());

        // Обработчик кнопки сохранения
        btnSave.setOnClickListener(v -> saveAndFinish());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (DatePicker view, int year1, int month1, int dayOfMonth) -> {
            selectedDate = year1 + "-" + (month1 + 1) + "-" + dayOfMonth;
            etTime.setText(selectedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void saveAndFinish() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("minMag", Double.parseDouble(etMinMag.getText().toString()));
        resultIntent.putExtra("lat", Double.parseDouble(etLat.getText().toString()));
        resultIntent.putExtra("lon", Double.parseDouble(etLon.getText().toString()));
        resultIntent.putExtra("time", selectedDate);

        // Передаём состояния переключателей
        resultIntent.putExtra("drawConstellations", switchDrawConstellations.isChecked());
        resultIntent.putExtra("drawLabels", switchDrawLabels.isChecked());
        resultIntent.putExtra("drawGrid", switchDrawGrid.isChecked());
        resultIntent.putExtra("drawHorizon", switchDrawHorizon.isChecked());
        resultIntent.putExtra("drawStars", switchDrawStars.isChecked());

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
        super.onBackPressed();
    }
}
