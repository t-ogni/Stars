package com.yakovskij.stars;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.yakovskij.stars.DataObjects.ConstellationInfo;

import java.util.List;

public class ConstellationDialogFragment extends DialogFragment {

    private static final String ARG_CONST_ID = "constId";
    private static final String ARG_CONST_NAME = "constName";

    private TextView tvConstellationInfo;
    private ImageView ivConstellationImage;
    private int constId;
    private String constName;

    public static ConstellationDialogFragment newInstance(int constId, String constName) {
        ConstellationDialogFragment fragment = new ConstellationDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CONST_ID, constId);
        args.putString(ARG_CONST_NAME, constName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            constId = getArguments().getInt(ARG_CONST_ID, -1);
            constName = getArguments().getString(ARG_CONST_NAME, "Неизвестное созвездие");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_constellation_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        tvConstellationInfo = view.findViewById(R.id.tvConstellationInfo);
        ivConstellationImage = view.findViewById(R.id.ivConstellationImage);

        toolbar.setNavigationOnClickListener(v -> dismiss()); // Закрытие окна

        // Загружаем информацию
        StarMapApiClient.getInstance(getContext()).getConstellationInfo(constId, new StarMapApiClient.Callback<List<ConstellationInfo>>() {
            @Override
            public void onSuccess(List<ConstellationInfo> result) {
                if (!result.isEmpty()) {
                    ConstellationInfo info = result.get(0);
                    getActivity().runOnUiThread(() -> {
                        String text = "Название созвездия: " + constName + "\n" +
                                "Ярчайшая звезда: " + info.brightest_star + "\n" +
                                "Мифология: " + info.meaning_mythology + "\n" +
                                "Первое упоминание: " + info.first_appearance + "\n" +
                                "Площадь неба: " + info.area_of_sky + "\n" +
                                "Лучшее время для наблюдения: " + info.best_time_to_see + "\n" +
                                "Небесное полушарие: " + info.celestial_hemisphere;
                        tvConstellationInfo.setText(text);
                    });

                    if (info.picture_file != null && !info.picture_file.isEmpty()) {
                        StarMapApiClient.getInstance(getContext()).getFileData(info.picture_file, new StarMapApiClient.Callback<String>() {
                            @Override
                            public void onSuccess(String base64Data) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                        ivConstellationImage.setImageBitmap(bitmap);
                                    } catch (Exception e) {
                                        Toast.makeText(getContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(Throwable t) {
                                Toast.makeText(getContext(), "Ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(getContext(), "Информация о созвездии не найдена", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }

            @Override
            public void onError(Throwable t) {
                Toast.makeText(getContext(), "Ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show();
                dismiss();
            }
        });
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
}
