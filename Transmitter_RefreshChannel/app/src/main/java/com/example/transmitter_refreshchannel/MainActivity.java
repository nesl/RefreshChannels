package com.example.transmitter_refreshchannel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("transmitter_refreshchannel");
    }


    private static final String TAG = "Transmitter";
    private SurfaceHolder surfaceHolder;

    EditText editText_Interval, editText_NoBits;
    TextView textView_Status, textView_DataID;
    private int[] data;
    private int[][] data_NRound;
    private int dataID = -1;
    String dateStr = new SimpleDateFormat("yyyy-MM-dd",
            Locale.getDefault()).format(new Date());
    String timestamp;
    static int round=0;
    Handler handler = new Handler();

    private int listener_refreshrate = 0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText_Interval   = findViewById(R.id.editTextInterval);
        textView_Status = findViewById(R.id.textViewStatus);
        textView_DataID = findViewById(R.id.textViewDataID);

        requestOverlayModePermission();



    }

    private void requestOverlayModePermission() {
        if (!Settings.canDrawOverlays(this)) {
            Context context = getBaseContext();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public native String StartReceiving(TextView textView_Status, MainActivity MainActivity);



    public void buttonStartTransmitting(View view){
        int interval_ms = Integer.valueOf(editText_Interval.getText().toString());

        editText_Interval.setEnabled(false);

        Intent intent = new Intent(this, FloatingSurfaceService.class);
        startForegroundService(intent);
        StartReceiving(textView_Status, this);

    }


    public void buttonStopTransmitting(View view){
        handler.removeCallbacksAndMessages(null);
        editText_Interval.setEnabled(true);
        round = 0;

        Intent intent = new Intent(this, FloatingSurfaceService.class);
        stopService(intent);
    }

    public void buttonResetDataID(View view){


    }

    private void writeToFile(String data) {
        try {
            dataID++;
            textView_DataID.setText(String.valueOf(dataID));

            String filePath = dateStr + "_"
                    + editText_Interval.getText().toString()
                    +  "_transmitter.txt";
            Log.e(TAG, "DataID: "+String.valueOf(dataID) + " Saving to "+ getExternalFilesDir(null) + "/" + filePath);
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

    boolean listener_refreshrate_end_received = false;

    private void RefreshRateListener(int listener_refreshrate) {

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("listener_refreshrate", listener_refreshrate);
        editor.apply();

        if (listener_refreshrate == 48){
            listener_refreshrate_end_received = true;
        }
        if (listener_refreshrate == 30 || listener_refreshrate == 24){
            listener_refreshrate_end_received = false;
        }
        editor.putBoolean("listener_refreshrate_end_received", listener_refreshrate_end_received);
        editor.apply();




    }


}