package com.oussama.blueshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.oussama.blueshare.tools.StorageTools;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView imageViewSend = findViewById(R.id.sendButton);
        imageViewSend.setOnClickListener((v)->toSend(v));

        ImageView imageViewReceive = findViewById(R.id.receiveButton);
        imageViewReceive.setOnClickListener(v->toReceive(v));

        Button folder = findViewById(R.id.folder);
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StorageTools.opendirectory(MainActivity.this);
            }
        });
    }
    public void toSend(View v){
        Intent intent = new Intent(MainActivity.this,SendActivity.class);
        startActivity(intent);
    }
    public void toReceive(View v){
        Intent intent = new Intent(MainActivity.this,ReceiveActivity.class);
        startActivity(intent);
    }
}