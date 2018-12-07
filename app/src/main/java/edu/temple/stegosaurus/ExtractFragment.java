package edu.temple.stegosaurus;


import android.Manifest;
import android.content.ActivityNotFoundException;
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
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
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
    String keys[], stegoImageLink, clientKey, serverKey;
    Uri stegoImageUri;
    File outputFile;

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
        context = getActivity();

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

                // generate keys
                String password = extractKeyEditText.getText().toString();
                stegoImageLink = selectPhotoEditText.getText().toString();

                // ensure inputs
                if ((stegoImageLink.equals("") && stegoImageUri == null) || password.equals(""))
                    Toast.makeText(getActivity(), "Please provide all inputs", Toast.LENGTH_SHORT).show();

                else {
                    String keys[] = EncryptionUtils.generateKeys(password);
                    clientKey = keys[0];
                    serverKey = keys[1];

                    extractData();
                }
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

            Call<ResponseBody> call;

            // always prefer image link to image file
            if(!stegoImageLink.equals(""))
                call = client.extractDataWithLink(stegoImageLink, serverKey);
            else
                call = client.extractDataWithImage(prepareFilePart("image", stegoImageUri), serverKey);

            // tells us the api's response
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    boolean success = writeResponseBodyToDisk(response.body());
                    //openFile();  We'll save this for later
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

    // Code from: https://futurestud.io/tutorials/retrofit-2-how-to-download-files-from-server
    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            String type = body.contentType().type();
            String extension;
            if(type.equals("text"))
                extension = "txt";
            else
                extension = "png";
            Log.i("madeit", "real type: " + type);

            File inputFile = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), "dataImage." + extension);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(inputFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);

                    // decrypt data
                    outputFile = new File(getActivity().getExternalFilesDir(null) + "/decrypted." + fileExt(inputFile.getPath()));
                    Log.i("madeit", "input: " + inputFile);
                    Log.i("madeit", "output: " + outputFile);


                    // TODO: get filename from the server
                    try {   // try to decrypt the data
                        EncryptionUtils.decrypt(clientKey, inputFile, outputFile);
                        Log.i("madeit", "outputFile: " + outputFile);

                    } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "Sorry, we couldn't decrypt the data", Toast.LENGTH_SHORT).show();
                    }

                    Log.i("path", "output: " + outputFile.getPath());
                    Log.i("path", "fileExt: " + fileExt(outputFile.getPath()));
                    // link: https://stegosaurus.ml/img/1544208381592.png

                    //SET IMAGE OR SHOW TEXT
                    if(fileExt(outputFile.getPath()).equals("txt")) {
                        int length = (int) outputFile.length();

                        byte[] bytes = new byte[length];

                        FileInputStream in = new FileInputStream(outputFile);
                        try {
                            in.read(bytes);
                        } finally {
                            in.close();
                        }

                        String contents = new String(bytes);
                        Toast.makeText(getActivity(), contents, Toast.LENGTH_SHORT).show();

                    }
                    else {
                        Message msg = Message.obtain();
                        msg.obj = outputFile.toString();
                        imageViewHandler.sendMessage(msg);
                    }
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

    public void openFile() {
        if (outputFile != null) {
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            Intent newIntent = new Intent(Intent.ACTION_VIEW);
            String mimeType = myMime.getMimeTypeFromExtension(fileExt(outputFile.getPath()));
            Log.i("path", "mimetype: " + mimeType);


            newIntent.setDataAndType(Uri.fromFile(outputFile), mimeType);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                getActivity().startActivity(newIntent);
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Couldn't launch activity", Toast.LENGTH_LONG).show();
            }
        }
        else
            Log.i("issue: ", "output doesn't yet exist");
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
