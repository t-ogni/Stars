package com.yakovskij.stars;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yakovskij.stars.StarMapAPI;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class StarMapApiClient {


    private static final String BASE_URL = "https://server2.sky-map.org/";
    private static StarMapApiClient instance;
    private final StarMapAPI api;
    private final Context context;
    private final Gson gson = new Gson();

    // Имена файлов для кэша
    private static final String CONSTELLATION_LIST_CACHE = "constellation_list.json";
    private static final String CONSTELLATION_LINES_CACHE_PREFIX = "constellation_lines_";
    // Имена файлов для кэша для каждого метода
    private static final String CONSTELLATION_INFO_CACHE_PREFIX = "constellation_info_";
    private static final String STAR_INFO_CACHE_PREFIX = "star_info_";
    private static final String FILE_DATA_CACHE_PREFIX = "file_data_";
    private static final String STAR_COORDINATES_CACHE_PREFIX = "star_coordinates_";

    private StarMapApiClient(Context context) {
        this.context = context.getApplicationContext();
        OkHttpClient client = new OkHttpClient.Builder()
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true; // ⚠️ Отключает проверку хоста (НЕБЕЗОПАСНО)
                    }
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(StarMapAPI.class);
    }

    public static synchronized StarMapApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new StarMapApiClient(context);
        }
        return instance;
    }

    public StarMapAPI getApi() {
        return api;
    }

    // Получение кэша из файла для списка созвездий
    public List<DataObjects.Constellation> loadConstellationListFromCache() {
        try {
            File file = new File(context.getFilesDir(), CONSTELLATION_LIST_CACHE);
            if (!file.exists()) return null;
            FileReader reader = new FileReader(file);
            Type listType = new TypeToken<List<DataObjects.Constellation>>() {}.getType();
            List<DataObjects.Constellation> list = gson.fromJson(reader, listType);
            reader.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Сохранение списка созвездий в файл
    public void saveConstellationListToCache(List<DataObjects.Constellation> list) {
        try {
            File file = new File(context.getFilesDir(), CONSTELLATION_LIST_CACHE);
            FileWriter writer = new FileWriter(file);
            gson.toJson(list, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Формирование имени файла кэша для линий созвездия
    private String getConstellationLinesCacheFileName(int constId, double lat, double lon, String tsUtc) {
        return CONSTELLATION_LINES_CACHE_PREFIX + constId + "_" + lat + "_" + lon + "_" + tsUtc + ".json";
    }

    // Загрузка линий созвездия из кэша
    public List<DataObjects.ConstellationLine> loadConstellationLinesFromCache(int constId, double lat, double lon, String tsUtc) {
        try {
            String fileName = getConstellationLinesCacheFileName(constId, lat, lon, tsUtc);
            File file = new File(context.getFilesDir(), fileName);
            if (!file.exists()) return null;
            FileReader reader = new FileReader(file);
            Type listType = new TypeToken<List<DataObjects.ConstellationLine>>() {}.getType();
            List<DataObjects.ConstellationLine> list = gson.fromJson(reader, listType);
            reader.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Сохранение линий созвездия в файл
    public void saveConstellationLinesToCache(int constId, double lat, double lon, String tsUtc, List<DataObjects.ConstellationLine> list) {
        try {
            String fileName = getConstellationLinesCacheFileName(constId, lat, lon, tsUtc);
            File file = new File(context.getFilesDir(), fileName);
            FileWriter writer = new FileWriter(file);
            gson.toJson(list, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Получение линий созвездия (с кэшированием)
    public void getConstellationLines(int constId, double lat, double lon, String tsUtc,
                                          final Callback<List<DataObjects.ConstellationLine>> callback) {
        List<DataObjects.ConstellationLine> cached = loadConstellationLinesFromCache(constId, lat, lon, tsUtc);
        if (cached != null && !cached.isEmpty()) {
            callback.onSuccess(cached);
            return;
        }

        api.computeConstellationLines(constId, lat, lon, tsUtc).enqueue(new retrofit2.Callback<List<DataObjects.ConstellationLine>>() {
            @Override
            public void onResponse(retrofit2.Call<List<DataObjects.ConstellationLine>> call,
                                   retrofit2.Response<List<DataObjects.ConstellationLine>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DataObjects.ConstellationLine> list = response.body();
                    saveConstellationLinesToCache(constId, lat, lon, tsUtc, list);
                    callback.onSuccess(list);
                } else {
                    callback.onError(new Exception("Response error: " + response.code()));
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<DataObjects.ConstellationLine>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    // --------------------- Кэширование для getConstellationInfo ---------------------

    private String getConstellationInfoCacheFileName(int constId) {
        return CONSTELLATION_INFO_CACHE_PREFIX + constId + ".json";
    }

    public List<DataObjects.ConstellationInfo> loadConstellationInfoFromCache(int constId) {
        try {
            File file = new File(context.getFilesDir(), getConstellationInfoCacheFileName(constId));
            if (!file.exists()) return null;
            FileReader reader = new FileReader(file);
            Type listType = new TypeToken<List<DataObjects.ConstellationInfo>>() {}.getType();
            List<DataObjects.ConstellationInfo> list = gson.fromJson(reader, listType);
            reader.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveConstellationInfoToCache(int constId, List<DataObjects.ConstellationInfo> list) {
        try {
            File file = new File(context.getFilesDir(), getConstellationInfoCacheFileName(constId));
            FileWriter writer = new FileWriter(file);
            gson.toJson(list, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getConstellationInfo(int constId, final Callback<List<DataObjects.ConstellationInfo>> callback) {
        List<DataObjects.ConstellationInfo> cached = loadConstellationInfoFromCache(constId);
        if (cached != null && !cached.isEmpty()) {
            callback.onSuccess(cached);
            return;
        }
        api.getConstellationInfo(constId).enqueue(new retrofit2.Callback<List<DataObjects.ConstellationInfo>>() {
            @Override
            public void onResponse(retrofit2.Call<List<DataObjects.ConstellationInfo>> call,
                                   retrofit2.Response<List<DataObjects.ConstellationInfo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveConstellationInfoToCache(constId, response.body());
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Response error: " + response.code()));
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<DataObjects.ConstellationInfo>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    // --------------------- Кэширование для getStarInfo ---------------------

    private String getStarInfoCacheFileName(int hipId) {
        return STAR_INFO_CACHE_PREFIX + hipId + ".json";
    }

    public List<DataObjects.StarInfo> loadStarInfoFromCache(int hipId) {
        try {
            File file = new File(context.getFilesDir(), getStarInfoCacheFileName(hipId));
            if (!file.exists()) return null;
            FileReader reader = new FileReader(file);
            Type listType = new TypeToken<List<DataObjects.StarInfo>>() {}.getType();
            List<DataObjects.StarInfo> list = gson.fromJson(reader, listType);
            reader.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveStarInfoToCache(int hipId, List<DataObjects.StarInfo> list) {
        try {
            File file = new File(context.getFilesDir(), getStarInfoCacheFileName(hipId));
            FileWriter writer = new FileWriter(file);
            gson.toJson(list, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getStarInfo(int hipId, final Callback<List<DataObjects.StarInfo>> callback) {
        List<DataObjects.StarInfo> cached = loadStarInfoFromCache(hipId);
        if (cached != null && !cached.isEmpty()) {
            callback.onSuccess(cached);
            return;
        }
        api.getStarInfo(hipId).enqueue(new retrofit2.Callback<List<DataObjects.StarInfo>>() {
            @Override
            public void onResponse(retrofit2.Call<List<DataObjects.StarInfo>> call,
                                   retrofit2.Response<List<DataObjects.StarInfo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveStarInfoToCache(hipId, response.body());
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Response error: " + response.code()));
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<DataObjects.StarInfo>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    // --------------------- Кэширование для getFileData ---------------------

    private String getFileDataCacheFileName(String fileName) {
        return FILE_DATA_CACHE_PREFIX + fileName.replaceAll("[^a-zA-Z0-9_.-]", "_") + ".txt";
    }

    public String loadFileDataFromCache(String fileName) {
        try {
            File file = new File(context.getFilesDir(), getFileDataCacheFileName(fileName));
            if (!file.exists()) return null;
            FileReader reader = new FileReader(file);
            char[] buffer = new char[(int) file.length()];
            reader.read(buffer);
            reader.close();
            return new String(buffer);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveFileDataToCache(String fileName, String fileData) {
        try {
            File file = new File(context.getFilesDir(), getFileDataCacheFileName(fileName));
            FileWriter writer = new FileWriter(file);
            writer.write(fileData);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getFileData(String fileName, final Callback<String> callback) {
        String cached = loadFileDataFromCache(fileName);
        if (cached != null && !cached.isEmpty()) {
            callback.onSuccess(cached);
            return;
        }
        api.getFileData(fileName).enqueue(new retrofit2.Callback<String>() {
            @Override
            public void onResponse(retrofit2.Call<String> call, retrofit2.Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveFileDataToCache(fileName, response.body());
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Response error: " + response.code()));
                }
            }
            @Override
            public void onFailure(retrofit2.Call<String> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    // --------------------- Кэширование для computeStarCoordinates ---------------------

    private String getStarCoordinatesCacheFileName(double minMag, double lat, double lon, String tsUtc) {
        return STAR_COORDINATES_CACHE_PREFIX +
                minMag + "_" + lat + "_" + lon + "_" + tsUtc.replaceAll("[:\\s-]", "") + ".json";
    }

    public List<DataObjects.Star> loadStarCoordinatesFromCache(double minMag, double lat, double lon, String tsUtc) {
        try {
            File file = new File(context.getFilesDir(), getStarCoordinatesCacheFileName(minMag, lat, lon, tsUtc));
            if (!file.exists()) return null;
            FileReader reader = new FileReader(file);
            Type listType = new TypeToken<List<DataObjects.Star>>() {}.getType();
            List<DataObjects.Star> list = gson.fromJson(reader, listType);
            reader.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveStarCoordinatesToCache(double minMag, double lat, double lon, String tsUtc, List<DataObjects.Star> list) {
        try {
            File file = new File(context.getFilesDir(), getStarCoordinatesCacheFileName(minMag, lat, lon, tsUtc));
            FileWriter writer = new FileWriter(file);
            gson.toJson(list, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getStarCoordinates(double minMag, double lat, double lon, String tsUtc,
                                       final Callback<List<DataObjects.Star>> callback) {
        List<DataObjects.Star> cached = loadStarCoordinatesFromCache(minMag, lat, lon, tsUtc);
        if (cached != null && !cached.isEmpty()) {
            callback.onSuccess(cached);
            return;
        }
        api.computeStarCoordinates(minMag, lat, lon, tsUtc).enqueue(new retrofit2.Callback<List<DataObjects.Star>>() {
            @Override
            public void onResponse(retrofit2.Call<List<DataObjects.Star>> call, retrofit2.Response<List<DataObjects.Star>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveStarCoordinatesToCache(minMag, lat, lon, tsUtc, response.body());
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Response error: " + response.code()));
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<DataObjects.Star>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void getConstellationList(Callback<List<DataObjects.Constellation>> callback) {

    }

    // Callback-интерфейс для упрощения вызова
    public interface Callback<T> {
        void onSuccess(T result);

        default void onError(Context context, Throwable t) {
            t.printStackTrace();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show());
        }

        default void onError(Throwable t) {
            t.printStackTrace();
        }

    }
}
