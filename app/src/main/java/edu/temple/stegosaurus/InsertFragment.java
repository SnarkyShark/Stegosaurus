package edu.temple.stegosaurus;


import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.support.v4.content.CursorLoader;
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
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
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
    EditText keyText, messageText;
    ImageView baseImageView, dataImageView;
    Button basePhotoButton, dataPhotoButton, insertButton;
    Context context;
    String clientKey, serverKey;
    Uri baseImageUri, dataImageUri, dataUri, encryptedDataUri;
    boolean bigEnough;
    File outputFile;

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
        messageText = v.findViewById(R.id.msgEditText);
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

        // USER: INSERT
        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // generate keys
                String password = keyText.getText().toString();
                String message = messageText.getText().toString();

                // ensure inputs
                if (password.equals("") || (dataImageUri == null && message.equals("")) || baseImageUri == null)
                    Toast.makeText(getActivity(), "Please provide all inputs", Toast.LENGTH_SHORT).show();
                else {
                    File inputFile = new File(getActivity().getFilesDir().toString());

                    if(dataImageUri != null) {    // if inserting an image
                        Log.i("filetest", "it's a image file");
                        inputFile = new File(getRealPathFromURI(dataImageUri));
                    }
                    if(dataImageUri == null) // if inserting a text file (or, eventually, other data)
                    {

                        Log.i("filetest", "it's a text file");
                        File dataTextFile = new File(getActivity().getExternalFilesDir(null) + "/data.txt");

                        try {
                            FileOutputStream stream = new FileOutputStream(dataTextFile);
                            try {
                                stream.write(message.getBytes());
                            } finally {
                                stream.close();
                            }

                            dataUri = Uri.fromFile(dataTextFile);
                            Log.i("filetest", "dataUri: " + dataUri);
                            inputFile = new File(dataUri.getPath());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    String keys[] = EncryptionUtils.generateKeys(password);
                    clientKey = keys[0];
                    serverKey = keys[1];

                    // create input/output files & get their paths
                    outputFile = new File(getActivity().getExternalFilesDir(null) + "/encrypted." + fileExt(inputFile.getPath()));
                    Log.i("madeit", "input: " + inputFile);
                    Log.i("madeit", "output: " + outputFile);

                    try {   // try to encrypt the data
                        Log.i("madeit", "---reset---");

                        EncryptionUtils.encrypt(clientKey, inputFile, outputFile);
                        Log.i("madeit", "outputFile: " + outputFile);

                        encryptedDataUri = Uri.fromFile(outputFile);
                        Log.i("madeit", "encrypted: " + encryptedDataUri);

                        if (baseImageIsBigEnough()) {
                            insertFile();
                        }
                        else
                            Toast.makeText(getActivity(), "Sorry, the base image file is too small", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Sorry, we couldn't encrypt the data", Toast.LENGTH_SHORT).show();
                    }
                }
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

    // inputs: baseImageUri, encryptedDataUri, serverKey
    // outputs: a link to an image file
    public void insertFile() {

        OkHttpClient.Builder timeClient = new OkHttpClient.Builder();
        timeClient.connectTimeout(30, TimeUnit.SECONDS);
        timeClient.readTimeout(30, TimeUnit.SECONDS);
        timeClient.writeTimeout(30, TimeUnit.SECONDS);

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(timeClient.build());
        Retrofit retrofit = builder.build();

        StegosaurusService client = retrofit.create(StegosaurusService.class);

        // ensure we have all needed inputs
        if(baseImageUri == null || encryptedDataUri == null)
            Toast.makeText(getActivity(), "Please provide all inputs", Toast.LENGTH_SHORT).show();
        else {
            if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                Log.i("madeit", "we have permission");
                Log.i("madeit", "base: " + baseImageUri);
                Log.i("madeit", "encrypted: " + encryptedDataUri);


                Call<String> call = client.insertData(prepareFilePart("image", baseImageUri), prepareFilePart("content", encryptedDataUri), serverKey);

                Log.i("madeit", "we called your mom");


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

    /**
     *  Helper functions
     */

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getActivity(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

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

            Log.i("dataCheck", "the height: " + imageHeight);
            Log.i("dataCheck", "width: " + imageWidth);

            int capacity = (imageHeight - (imageHeight % 32)) * (imageWidth - (imageWidth % 32));
            int size = (int) outputFile.length() * 8;   // bits to bytes


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
        RequestBody requestFile;

        // create RequestBody instance from file
        if(partName.equals("content")){
            requestFile =
                    RequestBody.create(
                            MediaType.parse("application/octet-stream"),
                            file
                    );
        }
        else {
            requestFile =
                    RequestBody.create(
                            MediaType.parse(getActivity().getContentResolver().getType(fileUri)),
                            file
                    );
        }

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

    private String fileExt(String url) {
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
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
