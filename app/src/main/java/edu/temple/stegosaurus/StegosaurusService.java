package edu.temple.stegosaurus;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface StegosaurusService {

    @GET("/api/test")
    Call<String> basicResponse();
}
