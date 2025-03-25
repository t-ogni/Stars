package com.yakovskij.stars;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final int SETTINGS_REQUEST_CODE = 1;
    private StarMapView starMapView;
    private FloatingActionButton FABSettings;

    private double minMag = 1.0;
    private double userLat = 30.0;
    private double userLon = 60.0;
    private String time = "2020-01-01";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        starMapView = findViewById(R.id.star_map_view);
        FABSettings = findViewById(R.id.floatingActionButton);

        FABSettings.setOnClickListener(v -> {
            starMapView.stop();
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            intent.putExtra("minMag", minMag);
            intent.putExtra("lat", userLat);
            intent.putExtra("lon", userLon);
            intent.putExtra("time", time);

            intent.putExtra("drawConstellations", starMapView.draw_constellations);
            intent.putExtra("drawLabels", starMapView.draw_labels);
            intent.putExtra("drawGrid", starMapView.draw_grid);
            intent.putExtra("drawHorizon", starMapView.draw_horizon);
            intent.putExtra("drawStars", starMapView.draw_stars);

            startActivityForResult(intent, SETTINGS_REQUEST_CODE);
        });


        // Загружаем звёзды при запуске
        loadStarsFromAssets();
    }

    private List<DataObjects.Star> loadStarsFromAssets() {
        Gson gson = new Gson();
        Type starListType = new TypeToken<ArrayList<DataObjects.Star>>(){}.getType();
        List<DataObjects.Star> stars = new ArrayList<>();

        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("stars4.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            stars = gson.fromJson(reader, starListType);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stars;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            minMag = data.getDoubleExtra("minMag", 1.0);
            userLat = data.getDoubleExtra("lat", 30.0);
            userLon = data.getDoubleExtra("lon", 60.0);
            time = data.getStringExtra("time");


            // Получаем дополнительные настройки (если они переданы)
            starMapView.draw_constellations = data.getBooleanExtra("drawConstellations", true);
            starMapView.draw_labels = data.getBooleanExtra("drawLabels", true);
            starMapView.draw_grid = data.getBooleanExtra("drawGrid", true);
            starMapView.draw_horizon = data.getBooleanExtra("drawHorizon", true);
            starMapView.draw_stars = data.getBooleanExtra("drawStars", true);

            loadApiOnStart();
        }
    }

    private void loadApiOnStart() {
        StarMapApiClient.getInstance(this).getStarCoordinates(minMag, userLat, userLon, time, new StarMapApiClient.Callback<List<DataObjects.Star>>() {
            @Override
            public void onSuccess(List<DataObjects.Star> result) {
                runOnUiThread(() -> {
                    starMapView.stop();
                    starMapView.stars = result;
                    starMapView.start();
                });
            }
            @Override
            public void onError(Context context, Throwable t) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        StarMapApiClient.getInstance(MainActivity.this)
                .getConstellationList(new StarMapApiClient.Callback<List<DataObjects.Constellation>>() {
                    @Override
                    public void onSuccess(List<DataObjects.Constellation> result) {
                        List<DataObjects.ConstellationLinesMapping> allLines = new ArrayList<>();
                        AtomicInteger counter = new AtomicInteger(result.size()); // Для отслеживания завершения всех запросов

                        for (DataObjects.Constellation constellation : result) {
                            StarMapApiClient.getInstance(MainActivity.this)
                                    .getConstellationLines(constellation.id, userLat, userLon, time, new StarMapApiClient.Callback<List<DataObjects.ConstellationLine>>() {
                                        @Override
                                        public void onSuccess(List<DataObjects.ConstellationLine> result) {
                                            synchronized (allLines) {
                                                allLines.add(new DataObjects.ConstellationLinesMapping(constellation, result) {
                                                });
                                            }

                                            if (counter.decrementAndGet() == 0) { // Ждём завершения всех запросов
                                                runOnUiThread(() -> {
                                                    starMapView.stop();
                                                    starMapView.constellationLines = allLines;
                                                    starMapView.start();
                                                });
                                            }
                                        }

                                        @Override
                                        public void onError(Context context, Throwable t) {
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.post(() -> Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(Context context, Throwable t) {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(() -> Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });



    }
}