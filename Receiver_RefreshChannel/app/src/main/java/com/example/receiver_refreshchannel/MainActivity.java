package com.example.receiver_refreshchannel;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

//import android.support.v4.content.LocalBroadcastManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.receiver_refreshchannel.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'receiver_refreshchannel' library on application startup.
    static {
        System.loadLibrary("receiver_refreshchannel");
    }

    private ActivityMainBinding binding;
    private static final String TAG = "Receiver";


    TextView textView_Status, textView_DataID;
    EditText editText_Interval;
    private int dataID = -1;
    String dateStr = new SimpleDateFormat("yyyy-MM-dd",
            Locale.getDefault()).format(new Date());
    String timestamp;


//    // broadcast
//    public static final String ACTION_TEST = "com.example.action.TEST";
//    private final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this.getApplicationContext());
//
//    private void onSendText(String text) {
//        final Intent intent = new Intent(ACTION_TEST).putExtra(Intent.EXTRA_TEXT, text);
//        broadcastManager.sendBroadcast(intent);
//    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        textView_Status = binding.textViewStatus;

        editText_Interval   = findViewById(R.id.editTextInterval);
        textView_DataID = findViewById(R.id.textViewDataID);


    }

    /**
     * A native method that is implemented by the 'receiver_refreshchannel' native library,
     * which is packaged with this application.
     */
    public native String StartReceiving(int interval, TextView textView_Status, MainActivity MainActivity);
    public native String StopReceiving();


    // Java
    public void buttonStartReceiving(View view) {
        int interval_ms = Integer.valueOf(editText_Interval.getText().toString());
        editText_Interval.setEnabled(false);
        StartReceiving(interval_ms, textView_Status, this);
//        textView_Status.setText();
    }

    public void buttonStopReceiving(View view) {
//        StopReceiving();
        textView_Status.setText(StopReceiving());
        editText_Interval.setEnabled(true);
    }

    public void buttonResetDataID(View view){
        dataID = -1;
        textView_DataID.setText(String.valueOf(dataID));
    }

    public void writeToFile(String data) {
        try {
            dataID++;
//            Log.e(TAG, "DataID: "+String.valueOf(dataID));

            textView_DataID.setText(String.valueOf(dataID));

            String filePath = dateStr + "_"
                    + editText_Interval.getText().toString()
                    +  "_receiver.txt";
            Log.e(TAG, "DataID: "+String.valueOf(dataID) +" Saving to "+ getExternalFilesDir(null) + "/" + filePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(
                    new File(getExternalFilesDir(null), filePath), true));
            timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            outputStreamWriter.write(timestamp + ";" + String.valueOf(dataID) + ";" + data + "\n");
            outputStreamWriter.close();

        } catch(IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Saving to file error!");
        }
    }

}