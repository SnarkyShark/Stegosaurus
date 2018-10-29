package edu.temple.stegosaurus;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * A simple {@link Fragment} subclass.
 */
public class EncryptFragment extends Fragment {

    Button button;
    TextView t;
    String message = "";
    boolean changed = false;

    public EncryptFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_encrypt, container, false);

        t = v.findViewById(R.id.encryptTime);

        button = v.findViewById(R.id.encryptButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test(); //something in test() makes the not-stored view disappear
                //changing t confuses the poor thing
            }
        });
    /*
        if (changed) {
            t.setText(message);
            changed = false;
        } */

        return v;
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
                        t.setText(stringBuilder.toString());
                        //changed = true;
                        //message = stringBuilder.toString();

                    } finally {
                        urlConnection.disconnect();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
