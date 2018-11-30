package edu.temple.stegosaurus;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class InsertFragment extends Fragment {

    Button basePhotoButton, dataPhotoButton, insertButton;
    TextView t;
    ImageView baseImageView, dataImageView;
    Uri baseImageUri, dataImageUri;
    View v;

    String testMessage = "test test";

    private static final int GET_PERMISSION = 99;
    private static final int PICK_BASE_IMAGE = 100;
    private static final int PICK_DATA_IMAGE = 101;

    public InsertFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_insert, container, false);
        t = v.findViewById(R.id.encryptTime);
        baseImageView = v.findViewById(R.id.basePhotoView);
        dataImageView = v.findViewById(R.id.dataPhotoView);
        basePhotoButton = v.findViewById(R.id.basePhotoButton);
        dataPhotoButton = v.findViewById(R.id.dataPhotoButton);
        insertButton = v.findViewById(R.id.insertButton);

        // GET PERMISSIONS
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                GET_PERMISSION);

        basePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_BASE_IMAGE);
            }
        });

        dataPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_DATA_IMAGE);
            }
        });

        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStegoImage();
            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == PICK_BASE_IMAGE) {
            baseImageUri = data.getData();
            //send it to baseImageHandler
            Message msg = Message.obtain();
            msg.obj = PICK_BASE_IMAGE;
            imageHandler.sendMessage(msg);
        }
        if(resultCode == RESULT_OK && requestCode == PICK_DATA_IMAGE) {
            dataImageUri = data.getData();
            //send it to baseImageHandler
            Message msg = Message.obtain();
            msg.obj = PICK_DATA_IMAGE;
            imageHandler.sendMessage(msg);
        }
    }

    /**
     *  Stegosaurus API functions
     */

    public void getStegoImage() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);
        if(baseImageUri != null && dataImageUri != null) {  // TODO: allow text file or photo THEN allow photo or other small file
            if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                //Call<String> call = client.howManyBytes(prepareFilePart("image", baseImageUri), false);
                Call<String> call = client.insertPhoto(prepareFilePart("image", baseImageUri),
                        prepareFilePart("content", dataImageUri), "random key");

                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        //Toast.makeText(getActivity(), "Code: " + response.code(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(getActivity(), "Response: " + response.body(), Toast.LENGTH_SHORT).show();
                        Log.i("Image Link:", response.body());
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Toast.makeText(getActivity(), "Can't insert image", Toast.LENGTH_SHORT).show();
                        Log.i("Throwable", t.toString());
                    }
                });
            }
            else
                Toast.makeText(getActivity(), "external storage: " + isExternalStorageWritable(), Toast.LENGTH_SHORT).show();

        }
        else
            Toast.makeText(getActivity(), "Please provide all inputs", Toast.LENGTH_SHORT).show();
    }

    public void getCapacity() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);
        if(baseImageUri != null) {
            if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Call<String> call = client.howManyBytes(prepareFilePart("image", baseImageUri), false);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        Message msg = Message.obtain();
                        msg.obj = response.body();
                        textViewHandler.sendMessage(msg);
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Toast.makeText(getActivity(), "Can't get capacity", Toast.LENGTH_SHORT).show();
                        Log.i("Throwable", t.toString());
                    }
                });
            }
            else
                Toast.makeText(getActivity(), "external storage: " + isExternalStorageWritable(), Toast.LENGTH_SHORT).show();

        }
    }

    /**
     *  Helper functions
     */

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        File file = FileUtils.getFile(getActivity(), fileUri);

        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse(getActivity().getContentResolver().getType(fileUri)),
                        file
                );

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    public void openGallery(int resultCode) {
        Intent gallery = new Intent (Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, resultCode);
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     *  Handlers
     */

    Handler textViewHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            t.setText((String) msg.obj);
            return false;
        }
    });

    Handler imageHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int imageRequestCode = (int) msg.obj;
            if (imageRequestCode == PICK_BASE_IMAGE)
                baseImageView.setImageURI(baseImageUri);
            if (imageRequestCode == PICK_DATA_IMAGE)
                dataImageView.setImageURI(dataImageUri);
            return false;
        }
    });
}
