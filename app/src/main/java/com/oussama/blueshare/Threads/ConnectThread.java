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

import com.oussama.blueshare.BluetoothTools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

public  class ConnectThread extends Thread {
    private  BluetoothSocket mmSocket;
    private  BluetoothDevice mmDevice;
    private  AppCompatActivity context;
    public  boolean isPairing= false;
    private String TAG="ConnectThread";
    public Uri Fileuri;


    @SuppressLint("MissingPermission")
    public ConnectThread(BluetoothDevice device, AppCompatActivity context) {
        this.context = context;
        BluetoothSocket tmp = null;
        mmDevice = device;
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
        if(isPairing)return;
        try {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }catch (Exception e){
            Log.d(TAG,"Couldn't cancel the discovery error:"+e.getMessage());
        }
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            isPairing = true;
            mmSocket.connect();

        } catch (IOException connectException) {
            Log.e(TAG, "Enable to Connect to socket "+connectException.getMessage());
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }
        manageConnectedSocket(mmSocket);
    }
    private  void manageConnectedSocket(BluetoothSocket socket)  {
        Log.d(TAG, "Connection accepted. Manage the socket.");
        StreamThread streamThread = new StreamThread(socket,context);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(Fileuri);
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null for the selected file.");
                streamThread.cancel();
            }
            streamThread.sendFile(inputStream);
            //    streamThread.cancel();
        } catch (FileNotFoundException e) {
            Log.d(TAG,e.getMessage());
            streamThread.cancel();
        }

    }

    public void cancel() {
        try {
            isPairing = false;
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}