package edu.temple.stegosaurus;


import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
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

public class InsertFragment extends Fragment {

    // View Elements
    View v;
    TextView t;
    EditText keyText;
    ImageView baseImageView, dataImageView;
    Button basePhotoButton, dataPhotoButton, insertButton;
    Context context;
    String keys[], clientKey, serverKey;
    Uri baseImageUri, dataImageUri;
    boolean bigEnough;

    private static final int PICK_BASE_IMAGE = 100;
    private static final int PICK_DATA_IMAGE = 101;

    public InsertFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // connect variables to layout
        v = inflater.inflate(R.layout.fragment_insert, container, false);
        t = v.findViewById(R.id.encryptTime);
        keyText = v.findViewById(R.id.keyEditText);
        baseImageView = v.findViewById(R.id.basePhotoView);
        dataImageView = v.findViewById(R.id.dataPhotoView);
        basePhotoButton = v.findViewById(R.id.basePhotoButton);
        dataPhotoButton = v.findViewById(R.id.dataPhotoButton);
        insertButton = v.findViewById(R.id.insertButton);
        context = getContext();
        bigEnough = false;

        // set default images
        baseImageView.setImageResource(R.drawable.file_logo);
        dataImageView.setImageResource(R.drawable.file_logo);

        // select base photo from user files
        basePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_BASE_IMAGE);
            }
        });

        // select data (photo) from user files
        dataPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_DATA_IMAGE);
            }
        });

        // send data to server
        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //checkCapacity();

                if(baseImageIsBigEnough())
                    insertImage();
                else
                    Toast.makeText(getActivity(), "Sorry, the base image file is too small", Toast.LENGTH_SHORT).show();

            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Sets imageViews
        if(resultCode == RESULT_OK && requestCode == PICK_BASE_IMAGE) {
            baseImageUri = data.getData();
            Message msg = Message.obtain();
            msg.obj = PICK_BASE_IMAGE;
            imageHandler.sendMessage(msg);
        }
        if(resultCode == RESULT_OK && requestCode == PICK_DATA_IMAGE) {
            dataImageUri = data.getData();
            Message msg = Message.obtain();
            msg.obj = PICK_DATA_IMAGE;
            imageHandler.sendMessage(msg);
        }
    }

    /**
     *  Stegosaurus API functions
     */

    // inputs: baseImageUri, dataImageUri, key
    // outputs: a link to an image file
    public void insertImage() {

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml/api/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);

        serverKey = "wonderful";

        // ensure we have all needed inputs
        if(baseImageUri == null || dataImageUri == null || serverKey.equals(""))
            Toast.makeText(getActivity(), "Please provide all inputs", Toast.LENGTH_SHORT).show();
        else {
            if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                Call<String> call = client.insertPhoto(prepareFilePart("image", baseImageUri), prepareFilePart("content", dataImageUri), serverKey);

                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        //Toast.makeText(getActivity(), response.code() + ": " + response.message(), Toast.LENGTH_SHORT).show();

                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("stego_link", response.body());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getActivity(), "Copied: " + response.body(), Toast.LENGTH_SHORT).show();
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
    }

    // get the storage capacity of an image
    public void checkCapacity() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml/api/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);
        if(baseImageUri != null && dataImageUri != null) {
            if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Call<String> call = client.getCapacity(prepareFilePart("image", baseImageUri), false);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        //Toast.makeText(getActivity(), "Storage: " + response.body(), Toast.LENGTH_SHORT).show();
                        int capacity = Integer.parseInt(response.body());

                        try {
                            AssetFileDescriptor afd = getActivity().getContentResolver().openAssetFileDescriptor(dataImageUri, "r");
                            long fileSize = afd.getLength();
                            afd.close();

                            Log.i("dataCheck", "capacity: " + capacity);
                            Log.i("dataCheck", "data: " + fileSize);

                            // checks whether it's big enough
                            bigEnough = capacity > fileSize;

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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

    // check if we can connect to the server
    public void pingServer() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml/api/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);
        Call<String> call = client.basicResponse();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Toast.makeText(getActivity(), "Response: " + response.body(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getActivity(), "Failed", Toast.LENGTH_SHORT).show();

            }
        });
    }

    /**
     *  Helper functions
     */

    public boolean baseImageIsBigEnough(){
        boolean retval = false;


        try {
            // getting height, width, and afd
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            ParcelFileDescriptor fd = getActivity().getContentResolver().openFileDescriptor(baseImageUri, "r");
            BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
            int imageWidth = options.outWidth;
            int imageHeight = options.outHeight;

            AssetFileDescriptor afd = getActivity().getContentResolver().openAssetFileDescriptor(dataImageUri, "r");

            Log.i("dataCheck", "the height: " + imageHeight);
            Log.i("dataCheck", "width: " + imageWidth);

            int capacity = (imageHeight - (imageHeight % 32)) * (imageWidth - (imageWidth % 32));
            int size = (int) afd.getLength() * 5;   // 5 is super arbitrary


            Log.i("dataCheck", "capacity: " + capacity);
            Log.i("dataCheck", "data: " + size);
            retval = capacity > size;   // the return value
            Log.i("dataCheck", "it's big enough: " + retval);

        } catch (Exception e) {
            e.printStackTrace();
        }


        return retval;
    }

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

    /**
     *  Handlers
     */

    // sets which images display in the imageViews
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
