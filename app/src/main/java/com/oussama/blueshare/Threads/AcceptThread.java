package com.oussama.blueshare.Threads;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.oussama.blueshare.BluetoothTools;

import java.io.IOException;
import java.util.UUID;

public  class AcceptThread extends Thread {
    private final String TAG="AcceptThread";
    private final BluetoothServerSocket mmServerSocket;
    private volatile boolean isRunning = false;

    private static AppCompatActivity context;
    @SuppressLint("MissingPermission")
    public AcceptThread(AppCompatActivity context) {
        this.context = context;

        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            UUID uuid = BluetoothTools.APP_UUID;
            tmp = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("BlueShareDevice1", uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        if(isRunning)return;
        BluetoothSocket socket = null;
        isRunning = true;
        // Keep listening until exception occurs or a socket is returned.
        while (isRunning) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                AcceptThread.manageConnectedSocket(socket);

                break;
            }
        }
    }


    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        isRunning = false;
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
    private static void manageConnectedSocket(BluetoothSocket socket) {
        // Start a new thread to manage the connected socket
        Log.d("Bluetooth", "Connection accepted. Manage the socket.");
        StreamThread streamThread = new StreamThread(socket,context);
        streamThread.start();


        // Add your logic for managing the connection
    }
}
