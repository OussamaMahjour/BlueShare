package com.oussama.blueshare.Threads;

import android.app.Activity;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.oussama.blueshare.tools.BluetoothTools;
import com.oussama.blueshare.R;
import com.oussama.blueshare.ReceiveActivity;
import com.oussama.blueshare.SendActivity;
import com.oussama.blueshare.tools.StorageTools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public  class StreamThread extends Thread{
    private static final String TAG="StreamThread";
    private static  Handler handler ;
    private final BluetoothSocket mmSocket;
    private static InputStream mmInStream;
    private static  OutputStream mmOutStream;
    private static byte[] mmBuffer = new byte[1024]; // mmBuffer store for the stream
    private interface Messages {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

    }
    private AppCompatActivity context;


    public StreamThread(BluetoothSocket socket,AppCompatActivity context) {
        this.context = context;
        handler = handler = new Handler(Looper.getMainLooper(), message -> {
            byte[] readBuf = (byte[]) message.obj;
            String Message = new String(readBuf, 0, message.arg1);
            switch (message.what){
                case Messages.MESSAGE_READ:
                    Log.d(TAG, "Data Received: "+Message);
                    break;
                case Messages.MESSAGE_WRITE:
                    Log.d(TAG,"Writing to socket "+Message);
                    break;
                case Messages.MESSAGE_TOAST:
                    Log.d(TAG,"Received message :"+Message);
                    break;
            }
            return true;
        });
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;


    }


    public void run() {

        mmBuffer = new byte[1024]; // Buffer size
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String fileName = null;
        long totalSize = 0; // Total size of the file
        long bytesReceived = 0; // Tracks bytes received

        try {
            // Step 1: Read the length of the filename
            byte[] lengthBuffer = new byte[4];
            mmInStream.read(lengthBuffer);
            int filenameLength = ByteBuffer.wrap(lengthBuffer).getInt();
            Log.d(TAG, "Filename length: " + filenameLength);

            // Step 2: Read the filename
            byte[] filenameBytes = new byte[filenameLength];
            mmInStream.read(filenameBytes);
            fileName = new String(filenameBytes, StandardCharsets.UTF_8);
            Log.d(TAG, "Filename received: " + fileName);

            if (fileName == null || fileName.isEmpty()) {
                Log.e(TAG, "Filename is missing or empty!");
                return;
            }

            // Step 3: Read the total file size
            byte[] sizeBuffer = new byte[8]; // Long size
            mmInStream.read(sizeBuffer);
            totalSize = ByteBuffer.wrap(sizeBuffer).getLong();
            Log.d(TAG, "File size: " + totalSize);

            // Step 4: Read the file content in chunks
            int numBytes;

            while (bytesReceived < totalSize) {
                numBytes = mmInStream.read(mmBuffer);
                if (numBytes == -1) break;

                byteArrayOutputStream.write(mmBuffer, 0, numBytes);
                bytesReceived += numBytes;

                // Calculate and log progress
                int percentage = (int) ((bytesReceived / (float) totalSize) * 100);
                Log.d(TAG, "Progress: " + percentage + "%");
                context.runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        ((ReceiveActivity)context).updateReceiving(percentage);
                    }
                });
            }

            // Step 5: File received completely
            Log.d(TAG, "File received successfully.");
            context.runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    ((TextView)context.findViewById(R.id.ReceiveFile)).setText("✓");
                }
            });
            byte[] fileData = byteArrayOutputStream.toByteArray();
            StorageTools.saveReceivedFile(fileData, fileName, context);

        } catch (IOException e) {
            Log.d(TAG, "Input stream was disconnected", e);
        } finally {
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing byte array output stream", e);
            }
        }
    }

    public static void sendMessage(String message,OutputStream outputStream){
        try {
            outputStream.write(message.getBytes());
            Message msg = handler.obtainMessage(Messages.MESSAGE_TOAST,-1,-1,mmBuffer);
            msg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);
        }
    }
    public static void listenForMessage(BluetoothSocket socket, AppCompatActivity context){
        StreamThread streamThread = new StreamThread(socket,context);
        streamThread.start();
    }



    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
            Message writtenMsg = handler.obtainMessage(Messages.MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg =
                    handler.obtainMessage(Messages.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }


    public void sendFile(InputStream inputStream,Uri Fileuri) {
        Timer timer = new Timer();

        // Schedule the task
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Running task at " + System.currentTimeMillis());
                // Your code here...
            }
        }, 0, 1000);
        try {
            String filename = StorageTools.getFileName(context,Fileuri);
            byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);

            // Send filename length and filename
            mmOutStream.write(ByteBuffer.allocate(4).putInt(filenameBytes.length).array());
            mmOutStream.flush();
            mmOutStream.write(filenameBytes);
            mmOutStream.flush();

            Log.d(TAG, "Sending File: " + filename);

            long fileSize = StorageTools.getFileSize(Fileuri,context);
            byte[] bytes = ByteBuffer.allocate(8).putLong(fileSize).array();
            mmOutStream.write(bytes);
            // Get the total file size
            int totalSize = inputStream.available();
            int bytesSent = 0;

            byte[] buffer = new byte[1024];
            int bytesRead;


            while ((bytesRead = inputStream.read(buffer)) != -1) {
                mmOutStream.write(buffer, 0, bytesRead);
                bytesSent += bytesRead;

                // Calculate percentage
                int percentage = (int) ((bytesSent / (float) totalSize) * 100);
                Log.d(TAG, "Progress: " + percentage + "%");
                context.runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        ((SendActivity)context).updateSending(percentage);
                    }
                });

            }
            mmOutStream.flush();


            Log.d(TAG, "File sent successfully.");

        } catch (IOException e) {
            Log.e(TAG, "Error occurred while sending file", e);
        }

    }



    // Call this method from the main activity to shut down the connection.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }


    }
}


