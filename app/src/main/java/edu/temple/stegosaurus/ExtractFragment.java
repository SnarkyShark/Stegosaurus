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


/**
 * A simple {@link Fragment} subclass.
 */
public class ExtractFragment extends Fragment {

    View v;
    TextView t;
    Button selectPhotoButton, extractButton;
    ImageView extractImageView;
    EditText extractKeyEditText, selectPhotoEditText;
    Uri stegoImageUri;
    String stegoImageLink;
    String[] keys;
    EncryptionUtils encryptBuddy;

    private static final int PICK_STEGO_IMAGE = 100;


    public ExtractFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_extract, container, false);
        t = v.findViewById(R.id.extractTextView);
        selectPhotoButton = v.findViewById(R.id.selectStegoButton);
        extractButton = v.findViewById(R.id.extractButton);
        extractImageView = v.findViewById(R.id.stegoImageView);
        extractKeyEditText = v.findViewById(R.id.extractKeyEditText);
        selectPhotoEditText = v.findViewById(R.id.imageUrlEditText);

        selectPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_STEGO_IMAGE);
            }
        });

        extractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stegoImageLink = selectPhotoEditText.getText().toString();
                keys = encryptBuddy.generateKeys(extractKeyEditText.getText().toString());
                sendStegoImage();
            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == PICK_STEGO_IMAGE) {
            stegoImageUri = data.getData();
            //send it to baseImageHandler
            Message msg = Message.obtain();
            msg.obj = PICK_STEGO_IMAGE;
            imageHandler.sendMessage(msg);
        }
    }

    /**
     *  Stegosaurus API functions
     */

    public void sendStegoImage() {

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml")
                .addConverterFactory(GsonConverterFactory.create(gson));
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);
        if(stegoImageUri != null || stegoImageLink != null) {
            if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                //Call<String> call = client.howManyBytes(prepareFilePart("image", baseImageUri), false);
                //Call<ResponseBody> call = client.extractDataWithImage(prepareFilePart("image", stegoImageUri), "rando key");
                Call<ResponseBody> call = client.extractDataWithLink(stegoImageLink, "rando key");

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
            else
                Toast.makeText(getActivity(), "external storage: " + isExternalStorageWritable(), Toast.LENGTH_SHORT).show();

        }
        else
            Toast.makeText(getActivity(), "Please provide all inputs", Toast.LENGTH_SHORT).show();
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

    // Code from: https://futurestud.io/tutorials/retrofit-2-how-to-download-files-from-server
    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            File futureStudioIconFile = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "dataImage.png");

            try {
                encryptBuddy.decrypt(keys[0], futureStudioIconFile, futureStudioIconFile);
                Log.i("encryptiontest", "decrypted");
            } catch(Exception e) {
                e.printStackTrace();
            }

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
            Toast.makeText(getActivity(),"set the image", Toast.LENGTH_SHORT).show();
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
