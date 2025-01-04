//package com.oussama.blueshare;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothServerSocket;
//import android.bluetooth.BluetoothSocket;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.pm.PackageManager;
//import android.database.Cursor;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.Message;
//import android.provider.OpenableColumns;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.android.material.bottomsheet.BottomSheetDialog;
//import com.oussama.blueshare.Threads.ConnectThread;
//import com.oussama.blueshare.Threads.StreamThread;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Scanner;
//import java.util.Set;
//import java.util.UUID;
//
//public class BluetoothTools {
//
//    public static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//    private static List<BluetoothDevice> devicesList = new ArrayList<>();
//    private static DevicesAdapter adapter;
//
//    public static int BLUETOOTH_CLIENT = 0;
//    public static int BLUETOOTH_SERVER = 1;
//
//    public static int REQUEST_ENABLE_BLUETOOTH = 200;
//    public static String TAG = "BluetoothTools";
//
//    public static Uri Fileuri ;
//    public static  BottomSheetDialog dialog;
//
//    public static UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//    private final static BroadcastReceiver receiver = new BroadcastReceiver() {
//        @SuppressLint("MissingPermission")
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            Log.d(TAG, "onReceive: ACTION FOUND.");
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
////                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
////                        Log.d(TAG,"couldn't get device name");
////                    return;
////                }
//                if (device != null && device.getName() != null) {
//                    Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
//                    devicesList.add(device);
//                    adapter.notifyDataSetChanged();
//                }
//            }
//        }
//    };
//
//    @SuppressLint("MissingPermission")
//    public static boolean enableBluetooth(AppCompatActivity context){
//        if (bluetoothAdapter == null) {
//            Log.d("BluetoothTools","Bluetooth Not supported");
//            return false;
//        }if (!bluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            context.startActivityForResult(enableBtIntent, 200);
//        }
//        return true;
//    }
//
//    public static void scanDevices(AppCompatActivity context,int activityCode){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            if (context.checkSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT}, 100);
//            }
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (context.checkSelfPermission( Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 101);
//            }
//        }
//        if (context.checkSelfPermission( Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_CONNECT}, 101);
//        }
//
//        adapter = new DevicesAdapter(devicesList, new DevicesAdapter.OnDeviceClickListener() {
//            @Override
//            public void onDeviceClick(BluetoothDevice device,View v) {
////                com.oussama.blueshare.Threads.ConnectThread connectThread = new com.oussama.blueshare.Threads.ConnectThread(device,context);
////                connectThread.Fileuri = Fileuri;
////                connectThread.start();
//
//
//                ((TextView)v.findViewById(R.id.isParing)).setVisibility(View.VISIBLE);
//            }
//        });
//
//        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//
//        if (pairedDevices.size() > 0) {
//            // There are paired devices. Get the name and address of each paired device.
//            for (BluetoothDevice device : pairedDevices) {
//                devicesList.add(device);
//                adapter.notifyDataSetChanged();
//
//            }
//        }
//
//
//        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
//
//        if(bluetoothAdapter.isDiscovering()){
//            bluetoothAdapter.cancelDiscovery();
//            Log.d(TAG, "btnDiscover: Canceling discovery.");
//
//            //check BT permissions in manifest
//            checkBTPermissions(context);
//
//            bluetoothAdapter.startDiscovery();
//            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//            context.registerReceiver(receiver, discoverDevicesIntent);
//        }
//        if(!bluetoothAdapter.isDiscovering()){
//
//            //check BT permissions in manifest
//            checkBTPermissions(context);
//
//            bluetoothAdapter.startDiscovery();
//            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//            context.registerReceiver(receiver, discoverDevicesIntent);
//        }
//        showBluetoothDevicesDialog(context);
//    }
//
//    private static void  checkBTPermissions(AppCompatActivity context) {
//        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
//            int permissionCheck = context.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
//            permissionCheck += context.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
//            if (permissionCheck != 0) {
//                context.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
//            }
//        }else{
//            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
//        }
//    }
//    private static void showBluetoothDevicesDialog(AppCompatActivity context) {
//        dialog = new BottomSheetDialog(context);
//        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_devices, null);
//        dialog.setContentView(view);
//        RecyclerView recyclerView = view.findViewById(R.id.devicesRecyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(context));
//        recyclerView.setAdapter(adapter);
//        dialog.show();
//    }
//
//
//}
