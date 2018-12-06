package edu.temple.stegosaurus;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.app.Activity.RESULT_OK;
import static edu.temple.stegosaurus.FileUtils.TAG;

public class ExtractFragment extends Fragment {

    // view elements
    View v;
    TextView t;
    EditText extractKeyEditText, selectPhotoEditText;
    ImageView extractImageView;
    Button selectPhotoButton, extractButton;
    Context context;
    String keys[], stegoImageLink, key;
    Uri stegoImageUri;

    private static final int PICK_STEGO_IMAGE = 100;

    public ExtractFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // connect variables to layout
        v = inflater.inflate(R.layout.fragment_extract, container, false);
        t = v.findViewById(R.id.extractTextView);
        extractKeyEditText = v.findViewById(R.id.extractKeyEditText);
        selectPhotoEditText = v.findViewById(R.id.imageUrlEditText);
        extractImageView = v.findViewById(R.id.stegoImageView);
        selectPhotoButton = v.findViewById(R.id.selectStegoButton);
        extractButton = v.findViewById(R.id.extractButton);

        // set default image
        extractImageView.setImageResource(R.drawable.file_logo);

        // select stego image from user files
        selectPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_STEGO_IMAGE);
            }
        });

        // send data to server
        extractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stegoImageLink = selectPhotoEditText.getText().toString();
                extractData();
            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // sets imageView
        if(resultCode == RESULT_OK && requestCode == PICK_STEGO_IMAGE) {
            stegoImageUri = data.getData();
            Message msg = Message.obtain();
            msg.obj = PICK_STEGO_IMAGE;
            imageHandler.sendMessage(msg);
        }
    }

    /**
     *  Stegosaurus API functions
     */

    // inputs: stego image uri OR stego image file, key
    // outputs: uri to data file
    public void extractData() {

        // create retrofit object
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml/api/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);

        // we have write permission
        if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            // set key
            key = extractKeyEditText.getText().toString();

            // ensure we have required inputs
            if ((stegoImageLink == null && stegoImageUri == null) || key.equals(""))
                Toast.makeText(getActivity(), "Please provide all inputs", Toast.LENGTH_SHORT).show();
            else {
                Call<ResponseBody> call;

                // always prefer image link to image file
                if(stegoImageLink != null)
                    call = client.extractDataWithLink(stegoImageLink, "rando key");
                else
                    call = client.extractDataWithImage(prepareFilePart("image", stegoImageUri), key);

                // tells us the api's response
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        boolean success = writeResponseBodyToDisk(response.body());
                        Toast.makeText(getActivity(), "File downloaded: " + success, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(getActivity(), "Can't extract data", Toast.LENGTH_SHORT).show();
                        Log.i("Throwable", t.toString());
                    }
                });
            }
        }
        else
            Toast.makeText(getActivity(), "external storage: " + isExternalStorageWritable(), Toast.LENGTH_SHORT).show();
    }

    /**
     *  Helper functions
     */

    // Convert a file uri to a MultipartBody.part
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

    // Open an intent to choose a picture from user's files
    public void openGallery(int resultCode) {
        Intent gallery = new Intent (Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, resultCode);
    }

    // ensures the app can write to external storage
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    // Code from: https://futurestud.io/tutorials/retrofit-2-how-to-download-files-from-server
    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            File futureStudioIconFile = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "dataImage.png");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                    //SET IMAGE
                    Message msg = Message.obtain();
                    msg.obj = futureStudioIconFile.toString();
                    imageViewHandler.sendMessage(msg);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     *  Handlers
     */

    Handler imageViewHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            extractImageView.setImageURI(Uri.parse((String) msg.obj));
            return false;
        }
    });

    Handler imageHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int imageRequestCode = (int) msg.obj;
            if (imageRequestCode == PICK_STEGO_IMAGE)
                extractImageView.setImageURI(stegoImageUri);
            return false;
        }
    });

}
