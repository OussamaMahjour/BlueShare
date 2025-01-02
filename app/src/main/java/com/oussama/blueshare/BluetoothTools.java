package com.oussama.blueshare;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class BluetoothTools {

    public static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static List<BluetoothDevice> devicesList = new ArrayList<>();
    private static DevicesAdapter adapter;

    public static int BLUETOOTH_CLIENT = 0;
    public static int BLUETOOTH_SERVER = 1;

    public static int REQUEST_ENABLE_BLUETOOTH = 200;
    public static String TAG = "BluetoothTools";

    public static Uri Fileuri ;
    public static  BottomSheetDialog dialog;
    private final static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                        Log.d(TAG,"couldn't get device name");
//                    return;
//                }
                if (device != null && device.getName() != null) {
                    Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                    devicesList.add(device);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    public static boolean enableBluetooth(AppCompatActivity context){
        if (bluetoothAdapter == null) {
            Log.d("BluetoothTools","Bluetooth Not supported");
            return false;
        }if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivityForResult(enableBtIntent, 200);

        }
        return true;
    }

    public static void scanDevices(AppCompatActivity context,int activityCode){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (context.checkSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT}, 100);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission( Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 101);
            }
        }
        if (context.checkSelfPermission( Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_CONNECT}, 101);
        }

        adapter = new DevicesAdapter(devicesList, new DevicesAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(BluetoothDevice device,View v) {


                ConnectThread connectThread = new ConnectThread(device,context);
                connectThread.start();
                ((TextView)v.findViewById(R.id.isParing)).setVisibility(View.VISIBLE);



            }
        });

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                devicesList.add(device);
                adapter.notifyDataSetChanged();

            }
        }


        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions(context);

            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            context.registerReceiver(receiver, discoverDevicesIntent);
        }
        if(!bluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions(context);

            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            context.registerReceiver(receiver, discoverDevicesIntent);
        }
        showBluetoothDevicesDialog(context);
    }

    private static void  checkBTPermissions(AppCompatActivity context) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = context.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += context.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                context.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
    private static void showBluetoothDevicesDialog(AppCompatActivity context) {
        dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_devices, null);
        dialog.setContentView(view);

        RecyclerView recyclerView = view.findViewById(R.id.devicesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        dialog.show();
    }
    public static class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private static AppCompatActivity context;



        public static boolean isPairing= false;

        public ConnectThread(BluetoothDevice device,AppCompatActivity context) {
            this.context = context;
            // Use a temporary object that is later assigned to mmSocket

            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e("BluetoothSocket", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            if(isPairing)return;

            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

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

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageConnectedSocket(mmSocket);
        }
        private static void manageConnectedSocket(BluetoothSocket socket)  {
            // Start a new thread to manage the connected socket
            Log.d("Bluetooth", "Connection accepted. Manage the socket.");
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



        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                isPairing = false;
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
    public static class StreamThread extends Thread{
        private static  Handler handler ;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream
        private interface MessageConstants {
            public static final int MESSAGE_READ = 0;
            public static final int MESSAGE_WRITE = 1;
            public static final int MESSAGE_TOAST = 2;

            // ... (Add other message types here as needed.)
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
                String filename = getFileName(context, Fileuri);
                byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);

                // Send filename length and filename
                mmOutStream.write(ByteBuffer.allocate(4).putInt(filenameBytes.length).array());
                mmOutStream.flush();
                mmOutStream.write(filenameBytes);
                mmOutStream.flush();

                Log.d(TAG, "Sending File: " + filename);

                long fileSize = getFileSize(Fileuri);
                byte[] bytes = ByteBuffer.allocate(8).putLong(fileSize).array();
                mmOutStream.write(bytes);
                // Get the total file size
                int totalSize = inputStream.available();
                int bytesSent = 0;

                byte[] buffer = new byte[1024];
                int bytesRead;
                dialog.dismiss();

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
                        ((TextView)context.findViewById(R.id.SendFile)).setText("✓");
                    }
                });

                Log.d(TAG, "File sent successfully.");
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred while sending file", e);
            }
        }
        private static void saveReceivedFile(byte[] fileData, String fileName, Activity context) {

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


    public static class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private volatile boolean isRunning = false;

        private static AppCompatActivity context;
        public AcceptThread(AppCompatActivity context) {
            this.context = context;

            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BlueShareDevice1", uuid);
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

}
