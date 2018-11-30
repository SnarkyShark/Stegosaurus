package edu.temple.stegosaurus;

import android.graphics.Bitmap;

import org.json.JSONObject;

import java.io.File;
import java.sql.Blob;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StegosaurusService {

    @GET("/api/test")
    Call<String> basicResponse();

    @POST("/api/test")
    Call<String> echoResponse(@Query("message") String message);

    @Multipart
    @POST("/api/get_capacity")
    Call<String> howManyBytes(@Part MultipartBody.Part theImage, @Query("formatted") boolean formatted);

    @Multipart
    @POST("/api/insert")
    Call<String> insertPhoto(@Part MultipartBody.Part baseImage, @Part MultipartBody.Part dataImage, @Query("key") String key);

    @Multipart
    @POST("/api/extract")
    Call<String> extractDataWithImage(@Part MultipartBody.Part baseImage, @Query("key") String key);
}
