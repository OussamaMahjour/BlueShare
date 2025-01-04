package com.oussama.blueshare.Threads;

import android.app.Activity;
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

import com.oussama.blueshare.BluetoothTools;
import com.oussama.blueshare.R;
import com.oussama.blueshare.ReceiveActivity;
import com.oussama.blueshare.SendActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public  class StreamThread extends Thread{
    private String TAG="StreamThread";
    private  Handler handler ;
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

    }
    private AppCompatActivity context;


    public StreamThread(BluetoothSocket socket,AppCompatActivity context) {
        this.context = context;
        handler = handler = new Handler(Looper.getMainLooper(), message -> {
            if (message.what == MessageConstants.MESSAGE_READ) {
                byte[] readBuf = (byte[]) message.obj;
                String receivedMessage = new String(readBuf, 0, message.arg1);
                Log.d("SocketData", "Received: " + receivedMessage);

                // Optionally update the UI with the received message
            }
            return true; // Indicates that the message has been handled
        });
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
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



                // Optionally update the UI with progress

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
            saveReceivedFile(fileData, fileName, context);

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



    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);

            // Share the sent message with the UI activity.
            Message writtenMsg = handler.obtainMessage(
                   MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg =
                    handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }
    private long getFileSize(Uri uri) {
        long fileSize = 0;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE);
                fileSize = cursor.getLong(sizeIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving file size: ", e);
        }
        return fileSize;
    }
    public String getFileName(Context context, Uri uri) {
        String fileName = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        // If the scheme is "file", use the path directly
        if (fileName == null) {
            fileName = new File(uri.getPath()).getName();
        }
        return fileName;
    }
    public void sendFile(InputStream inputStream) {
        try {
            String filename = getFileName(context, BluetoothTools.Fileuri);
            byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);

            // Send filename length and filename
            mmOutStream.write(ByteBuffer.allocate(4).putInt(filenameBytes.length).array());
            mmOutStream.flush();
            mmOutStream.write(filenameBytes);
            mmOutStream.flush();

            Log.d(TAG, "Sending File: " + filename);

            long fileSize = getFileSize(BluetoothTools.Fileuri);
            byte[] bytes = ByteBuffer.allocate(8).putLong(fileSize).array();
            mmOutStream.write(bytes);
            // Get the total file size
            int totalSize = inputStream.available();
            int bytesSent = 0;

            byte[] buffer = new byte[1024];
            int bytesRead;
            BluetoothTools.dialog.dismiss();

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
                // Optionally, update UI with progress (requires passing a callback or handler)

            }
            mmOutStream.flush();
            context.runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    ((SendActivity)context).flipAnimation();
                    //  ((TextView)context.findViewById(R.id.SendFile)).setText("✓");
                }
            });

            Log.d(TAG, "File sent successfully.");
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while sending file", e);
        }
    }
    private  void saveReceivedFile(byte[] fileData, String fileName, Activity context) {

        File directory;

// Use public Downloads directory
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BlueShare");
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: " + directory.getAbsolutePath());
                    // Fallback to internal storage
                    directory = new File(context.getFilesDir(), "BlueShare");
                    directory.mkdirs();
                }
            }
        } else {
            Log.e(TAG, "External storage not available. Using internal storage.");
            directory = new File(context.getFilesDir(), "BlueShare");
            directory.mkdirs();
        }

// Create the file
        File file = new File(directory, fileName);
        try {
            if (!file.exists() && file.createNewFile()) {
                Log.d(TAG, "File created: " + file.getAbsolutePath());
            } else {
                Log.d(TAG, "File already exists or couldn't be created.");
            }

            // Write data to the file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileData);
                Log.d(TAG, "File saved successfully to: " + file.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e(TAG, "Error saving file", e);
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


