package com.oussama.blueshare;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.oussama.blueshare.Threads.AcceptThread;
import com.oussama.blueshare.Threads.StreamThread;
import com.oussama.blueshare.databinding.ActivityReceiveBinding;
import com.oussama.blueshare.tools.BluetoothTools;

public class ReceiveActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityReceiveBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_receive);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.requestPermissions(new String[]{
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
        },12);
        BluetoothTools.enableBluetooth(this);
        ImageView receiveButton = findViewById(R.id.receiveLog);

        AcceptThread acceptThread = new AcceptThread(this,(socket)->{
            StreamThread streamThread  = new StreamThread(socket,ReceiveActivity.this);
            streamThread.start();
        });

        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);

        startActivityForResult(discoverableIntent, 1);

        receiveButton.setOnClickListener(v->{
            acceptThread.start();
            findViewById(R.id.ReceiveWaiting).setVisibility(View.VISIBLE);
            findViewById(R.id.ReceiveStart).setVisibility(View.INVISIBLE);
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},10);

        }
        );


    }
    public void updateReceiving(int persentage){
        findViewById(R.id.ReceiveWaiting).setVisibility(View.INVISIBLE);
        findViewById(R.id.ReceivingFile).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.ReceiveFile)).setText(persentage+"%");

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 12) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("BluePermission","Permission granted");
            } else {
                Log.d("BluePermission","Permission not granted");
            }
        }else if(requestCode == BluetoothTools.REQUEST_ENABLE_BLUETOOTH){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                BluetoothTools.bluetoothAdapter.enable();
                Log.d("BluePermission","Bluetooth Enabled");
            } else {
                Log.d("BluePermission","Couldn't Enable Bluetooth");
            }
        }
    }


}