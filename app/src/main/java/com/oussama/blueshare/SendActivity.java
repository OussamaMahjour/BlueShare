package com.oussama.blueshare;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.oussama.blueshare.Threads.ConnectThread;
import com.oussama.blueshare.Threads.StreamThread;
import com.oussama.blueshare.tools.BluetoothTools;
import com.oussama.blueshare.tools.StorageTools;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SendActivity extends AppCompatActivity {
    private Uri Fileuri;

    private static List<BluetoothDevice> devicesList = new ArrayList<>();
    private static DevicesAdapter adapter;
    public static BottomSheetDialog dialog;
    public static int percentage =0 ;
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {

                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Uri fileUri = result.getData().getData();
                                Fileuri = fileUri;
                                BluetoothTools.scanDevices(SendActivity.this,(device)->{
                                    if(!devicesList.contains(device)){
                                        devicesList.add(device);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                                showDevices();
                            }
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BluetoothTools.requestPermissions(this);
        BluetoothTools.enableBluetooth(this);

        adapter = new DevicesAdapter(devicesList, new DevicesAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(BluetoothDevice device,View v) {
               BluetoothTools.sendFile(Fileuri,device,SendActivity.this);
                v.findViewWithTag(device.getName()).setVisibility(View.VISIBLE);

            }
        });

        ImageView sendButton = findViewById(R.id.sendLogo);
        sendButton.setOnClickListener(v -> {
            devicesList.clear();
            StorageTools.openFilePicker(this,filePickerLauncher);

        });

        ImageView folder = findViewById(R.id.folder);
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StorageTools.opendirectory(SendActivity.this);
            }
        });




    }


    public  void updateSending(int persentage){

        findViewById(R.id.SendLogoLayout).setVisibility(View.INVISIBLE);
        findViewById(R.id.SendingFile).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.SendFile)).setText(SendActivity.percentage+"%");
        SendActivity.percentage = persentage;

    }
    private void showDevices(){
        dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_devices, null);
        dialog.setContentView(view);
        RecyclerView recyclerView = view.findViewById(R.id.devicesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        dialog.show();
    }
    public void flipCard(TextView view){
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(view, "rotationY", 0f, 90f);
        flipOut.setDuration(300); // Duration of the flip-out animation

        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // After flip-out is complete, change the text and flip back to show it
                view.setText("");
//                view.setRotationY(180);
                view.setBackground(ContextCompat.getDrawable(SendActivity.this,R.drawable.encheck));
                ObjectAnimator flipIn = ObjectAnimator.ofFloat(view, "rotationY", 270f, 360f);
                flipIn.setDuration(300); // Duration of the flip-in animation
                flipIn.start();
            }
        });

        flipOut.start();
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
                Log.d("BluePermission","Permission granted");
            } else {
                Log.d("BluePermission","Permission not granted");
            }
        }
    }



}