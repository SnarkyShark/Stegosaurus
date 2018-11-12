package edu.temple.stegosaurus;


import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * A simple {@link Fragment} subclass.
 */
public class EncryptFragment extends Fragment {

    Button button, testButton;
    TextView t;
    String message, key;
    EditText msg_box, key_box;
    boolean changed = false;
    View v;

    private static final int PICK_IMAGE = 123456;

    public EncryptFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_encrypt, container, false);

        message = "";
        key = "";
        t = v.findViewById(R.id.encryptTime);
        msg_box = v.findViewById(R.id.msgEditText);
        key_box = v.findViewById(R.id.keyEditText);

        button = v.findViewById(R.id.encryptButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testRetro();
            }
        });

        testButton = v.findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testInputs();
            }
        });

        return v;
    }

    public void testInputs() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml")
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();

        StegosaurusService client = retrofit.create(StegosaurusService.class);

        //get images
        //create call --> method
        //enqueue call
    }

    public void testRetro() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml")
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();

        StegosaurusService client = retrofit.create(StegosaurusService.class);
        Call<String> call = client.basicResponse();

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Message msg = Message.obtain();
                msg.obj = response.body();
                networkHandler.sendMessage(msg);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getActivity(), "something went wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    Handler networkHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            t.setText((String) msg.obj);
            return false;
        }
    });
}
