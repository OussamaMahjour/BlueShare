package com.oussama.blueshare.tools;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.oussama.blueshare.DevicesAdapter;
import com.oussama.blueshare.R;
import com.oussama.blueshare.SendActivity;
import com.oussama.blueshare.Threads.ConnectThread;
import com.oussama.blueshare.Threads.StreamThread;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class BluetoothTools {
    public static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public static String TAG = "BluetoothTools";
    public interface OnDeviceFound extends Consumer<BluetoothDevice>{};
    public static int REQUEST_ENABLE_BLUETOOTH = 200;
    public static UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static void sendFile(Uri file, BluetoothDevice device, AppCompatActivity context){
        ConnectThread connectThread = new ConnectThread(device,(socket)->{
            ((SendActivity)context).dialog.dismiss();
            StreamThread streamThread = new StreamThread(socket,context);
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(file);

                streamThread.sendFile(inputStream,file);
                if (inputStream == null) {
                    Log.e(ConnectThread.TAG, "InputStream is null for the selected file.");
                    streamThread.cancel();
                }
                streamThread.sendFile(inputStream,file);
            } catch (FileNotFoundException e) {
                Log.d(ConnectThread.TAG,e.getMessage());
                streamThread.cancel();
            }


        });
        connectThread.start();
    }
    @SuppressLint("MissingPermission")
    public static boolean enableBluetooth(AppCompatActivity context){
        if (bluetoothAdapter == null) {
            Log.d("BluetoothTools","Bluetooth Not supported");
            return false;
        }if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public static void scanDevices(AppCompatActivity context, OnDeviceFound onDeviceFound){

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive: ACTION FOUND.");
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (device != null && device.getName() != null) {
                        Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                            onDeviceFound.accept(device);
                    }
                }
            }
        };


        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Log.d(TAG, "btnDiscover: Looking for paired devices.");
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                onDeviceFound.accept(device);
            }
        }
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
        }
        bluetoothAdapter.startDiscovery();
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, discoverDevicesIntent);
    }
    public static void requestPermissions(AppCompatActivity context){
        ActivityCompat.requestPermissions(context, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 100);
    }
}
