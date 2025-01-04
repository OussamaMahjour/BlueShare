package com.oussama.blueshare.Threads;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.Tag;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.oussama.blueshare.tools.BluetoothTools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public  class ConnectThread extends Thread {
    public static  BluetoothSocket mmSocket;
    public static boolean isPaired= false;
    public static String TAG="ConnectThread";
    public interface OnConnect extends Consumer<BluetoothSocket>{};
    private final OnConnect onConnect ;



    @SuppressLint("MissingPermission")
    public ConnectThread(BluetoothDevice device,OnConnect onConnect) {
        this.onConnect = onConnect;
        BluetoothSocket tmp = null;
        UUID uuid = BluetoothTools.APP_UUID;
        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        if(isPaired) {
            try {
                mmSocket.close();
                isPaired=false;
            } catch (IOException e) {
                Log.d(TAG,"Couldn't Close the socket");
            }

        }

        try {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }catch (Exception e){
            Log.d(TAG,"Couldn't cancel the discovery error:"+e.getMessage());
        }
        try {
            mmSocket.connect();
            isPaired = true;
            onConnect.accept(mmSocket);

        } catch (IOException connectException) {
            Log.e(TAG, "Enable to Connect to socket "+connectException.getMessage());
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }
    }
    public void cancel() {
        try{
            mmSocket.close();
            isPaired=false;
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}