package edu.temple.stegosaurus;


import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
                test();
            }
        });

        testButton = v.findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendInputs();
                testRetro();
            }
        });

        return v;
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

    public void sendInputs() {

        message = msg_box.getText().toString();
        key = key_box.getText().toString();

        if (message.compareTo("") == 0 || key.compareTo("") == 0)
            Toast.makeText(getActivity(), "Please fill out all fields before encrypting", Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(getActivity(), "Cool. Encrypting...", Toast.LENGTH_SHORT).show();
        }


        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    public void test() {
        new Thread() {
            public void run() {
                try {
                    URL url = new URL("https://stegosaurus.ml/api/test");
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }
                        bufferedReader.close();
                        Message msg = Message.obtain();
                        msg.obj = stringBuilder.toString();
                        networkHandler.sendMessage(msg);
                    } finally {
                        urlConnection.disconnect();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK)
        {
            // https://stackoverflow.com/questions/5309190/android-pick-images-from-gallery
            //
        }
    }

    Handler networkHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            t.setText((String) msg.obj);
            return false;
        }
    });
}
