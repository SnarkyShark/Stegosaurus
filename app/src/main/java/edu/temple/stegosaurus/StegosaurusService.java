package edu.temple.stegosaurus;

import android.graphics.Bitmap;

import org.json.JSONObject;

import java.io.File;
import java.sql.Blob;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StegosaurusService {

    @Multipart
    @POST("insert")
    Call<String> insertData(@Part MultipartBody.Part image, @Part MultipartBody.Part content, @Query("key") String key);

    @Multipart
    @POST("extract")
    Call<ResponseBody> extractDataWithImage(@Part MultipartBody.Part baseImage, @Query("key") String key);

    @POST("extract")
    Call<ResponseBody> extractDataWithLink(@Query("image_url") String image_url, @Query("key") String key);
}
