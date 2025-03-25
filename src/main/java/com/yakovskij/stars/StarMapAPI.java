package com.yakovskij.stars;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StarMapAPI {

    @GET("computeConstellationLines")
    Call<List<DataObjects.ConstellationLine>> computeConstellationLines(
            @Query("constId") int constId,
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("tsUtc") String tsUtc
    );

    @GET("getConstellationInfo")
    Call<List<DataObjects.ConstellationInfo>> getConstellationInfo(
            @Query("constId") int constId
    );

    @GET("getStarInfo")
    Call<List<DataObjects.StarInfo>> getStarInfo(
            @Query("hipId") int hipId
    );

    @GET("getFileData")
    Call<String> getFileData(
            @Query("fileName") String fileName
    );

    @GET("computeStarCoordinates")
    Call<List<DataObjects.Star>> computeStarCoordinates(
            @Query("minMag") double minMag,
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("tsUtc") String tsUtc
    );
}
