package com.tomoyowasaki.forpass;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;

import java.util.UUID;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

public class MainActivity extends AppCompatActivity {
    private static final String macAddress = "89:51:12:36:28:11";
    private static final String ServiceUUID="55535343-fe7d-4ae5-8fa9-9fafd205e455";
    private static final String sendUUID="49535343-1e4d-4bd9-ba61-23c647249616";
    private static final int sendInterval=150;
    private boolean isConnected = false;
    RxBleClient rxBleClient;
    RxBleDevice device;
    private Button connectButton;
    private Button modeButton;
    private Button sendButton;
    private RxBleConnection connection;
    private TimePicker timePicker;
    Disposable disposable;
    private int mode = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        connectButton=findViewById(R.id.connectButton);
        modeButton=findViewById(R.id.modeButton);
        sendButton=findViewById(R.id.sendButton);
        timePicker=findViewById(R.id.timePicker);
            rxBleClient= RxBleClient.create(this);
    }
    public void onConnectButtonClicked(View view) {
        if(isConnected)
        {
            disposable.dispose();
            isConnected=false;
            device=null;
        }else {
            device = rxBleClient.getBleDevice(macAddress);
            disposable= device.establishConnection(false).subscribe(
                    rxBleConnection -> {
                        isConnected = true;
                        connection = rxBleConnection;
                        //Android Toast
                        connectButton.setText("Disconnect");
                        makeToast("Connection success");
                    },
                    throwable -> {
                        isConnected = false;
                        makeToast("Connection failed");
                    }
            );

        }
        //disposable.dispose();
    }

    public void onModeButtonClicked(View view) {
         if(mode==0)
         {
             modeButton.setText("Spinner");
             mode=1;
         }else {
             modeButton.setText("Clock");
             mode=0;
         }
    }
    public void onSendButtonClicked(View view) {
        if (device == null || !isConnected) {
        Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        }
        if(mode==0)
        {

            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            String time = String.format("@%d\n", (hour * 60 + minute));
            byte[] byteArray = time.getBytes();

            connection.writeCharacteristic(UUID.fromString(sendUUID), byteArray).subscribe(
                    aVoid -> {
                        makeToast("Send success");
                    },
                    throwable -> {
                        makeToast("Failed." + throwable.getMessage());
                    }
            );
        }else{
             Handler handler = new Handler();
             Runnable runnable = new Runnable() {
                 @Override
                 public void run() {
                     byte[] byteArray = String.format("@%d\n",timePicker.getMinute()).getBytes();
                     connection.writeCharacteristic(UUID.fromString(sendUUID), byteArray).subscribe(
                             aVoid -> {
                                 handler.postDelayed(this, sendInterval);
                             },
                             throwable -> {
                                 makeToast("Failed." + throwable.getMessage());
                             }
                     );

                 }
             };
             handler.postDelayed(runnable, sendInterval);
        }
    }
    public void makeToast(String text)
    {
        runOnUiThread(() -> Toast.makeText(this,text,Toast.LENGTH_SHORT).show());
    }
}