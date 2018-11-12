package edu.temple.stegosaurus;

import android.graphics.Bitmap;

import java.io.File;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface StegosaurusService {

    @GET("/api/test")
    Call<String> basicResponse();

    @Multipart
    @POST("/api/insert")
    Call<String> insertPhoto(@Part MultipartBody.Part baseImage, @Part MultipartBody.Part dataImage, @Part("KEY") RequestBody key);
}
