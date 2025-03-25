package com.yakovskij.stars;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.yakovskij.stars.DataObjects.StarInfo;

import java.util.List;

public class StarDialogFragment extends DialogFragment {

    private static final String ARG_HIP = "hip";
    private TextView tvStarInfo;
    private int hipId;

    public static StarDialogFragment newInstance(int hip) {
        StarDialogFragment fragment = new StarDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_HIP, hip);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_star_info, container, false);
        tvStarInfo = view.findViewById(R.id.tvStarInfo);
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);

        // Настройка кнопки "Назад"
        toolbar.setNavigationOnClickListener(v -> dismiss());

        // Получаем данные из аргументов
        if (getArguments() != null) {
            hipId = getArguments().getInt(ARG_HIP, -1);
        }

        if (hipId == -1) {
            Toast.makeText(getContext(), "Некорректный идентификатор звезды", Toast.LENGTH_SHORT).show();
            dismiss();
            return view;
        }

        // Загружаем информацию о звезде
        loadStarInfo(getContext(), hipId);

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.8));
            getDialog().getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
    }

    private void loadStarInfo(Context context, int hip) {
        StarMapApiClient.getInstance(context).getStarInfo(hip, new StarMapApiClient.Callback<List<StarInfo>>() {
            @Override
            public void onSuccess(List<StarInfo> result) {
                if (!result.isEmpty()) {
                    StarInfo info = result.get(0);
                    requireActivity().runOnUiThread(() -> {
                        String text = "Название: " + info.name + "\n" +
                                "Созвездие: " + info.constellation + "\n" +
                                "Видимая величина: " + info.magnitude + "\n" +
                                "Температура (K): " + info.temperature_kelvin + "\n" +
                                "Спектральный класс: " + info.spectral_class + "\n" +
                                "Правое восхождение: " + info.right_ascension + "\n" +
                                "Склонение: " + info.declination + "\n" +
                                "Расстояние (пк): " + info.distance_parsecs + "\n" +
                                "Bayer/Flamsteed: " + info.bayer_flamsteed;

                        tvStarInfo.setText(text);
                    });
                } else {
                    onError(context, new Exception("Информация о звезде не найдена"));
                }
            }

            @Override
            public void onError(Context context, Throwable t) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(context, "Ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }
}
