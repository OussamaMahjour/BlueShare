package com.oussama.blueshare.Threads;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.oussama.blueshare.SendActivity;
import com.oussama.blueshare.tools.BluetoothTools;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

public  class AcceptThread extends Thread {
    private static final String TAG="AcceptThread";
    private final BluetoothServerSocket mmServerSocket;
    public static BluetoothSocket mmSocket;
    private static boolean  isPaired= false;
    private AppCompatActivity context;

    /**
     * the function that will be running ones the connections is established
     * */
    public interface OnAccept extends Consumer<BluetoothSocket>{};

    private OnAccept onAccept;

    @SuppressLint("MissingPermission")
    public AcceptThread(AppCompatActivity context,OnAccept onAccept) {
        BluetoothServerSocket tmp = null;
        this.context = context;
        this.onAccept = onAccept;
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
        if(isPaired){
            try {
                mmServerSocket.close();
                isPaired = false;


            } catch (IOException e) {
                Log.d(TAG,"Couldn't Close the server socket");
            }
        }
        BluetoothSocket socket = null;
        try {
            socket = mmServerSocket.accept();
            isPaired = true;
            onAccept.accept(socket);
        } catch (IOException e) {
            Log.e(TAG, "Socket's accept() method failed", e);
            return;
        }
    }
    public void cancel() {
        try {
            mmServerSocket.close();
            isPaired = false;
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
    public  static BluetoothSocket getConnectedSocket() {
        Log.d(TAG, "Connection accepted. Manage the socket.");
        if(isPaired){
            return mmSocket;
        }
        else{
            return null;
        }


    }
}
