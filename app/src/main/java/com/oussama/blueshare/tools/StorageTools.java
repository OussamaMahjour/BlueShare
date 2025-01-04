package com.oussama.blueshare.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;


import com.oussama.blueshare.SendActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class StorageTools {
    private static final String TAG = "StorageTools";
    private static Uri FileUri;

    public static Uri openFilePicker(AppCompatActivity context, ActivityResultLauncher<Intent> filePickerLauncher) {
        // Create an intent to pick files
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Set the type of files you want to allow
        intent.addCategory(Intent.CATEGORY_OPENABLE); // Only show files that can be opened

        // Launch the file picker
        filePickerLauncher.launch(intent);
        return FileUri;
    }

    public static void saveFile(byte[] fileData,AppCompatActivity context,String fileName){
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

    public static String getFileName(Context context, Uri uri) {
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
    public static  void saveReceivedFile(byte[] fileData, String fileName, Activity context) {

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



    public static long getFileSize(Uri uri,AppCompatActivity context) {
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

}
