package com.oussama.blueshare;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder> {

    private final List<BluetoothDevice> devices;
    private final OnDeviceClickListener onDeviceClickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device,View v);
    }
    public DevicesAdapter(List<BluetoothDevice> devices,OnDeviceClickListener listener) {

    this.devices = devices;
    this.onDeviceClickListener = listener;
    }


@NonNull
@Override
public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_holder, parent, false);
    return new DeviceViewHolder(view);
}

@Override
public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
    holder.deviceNameTextView.setText(devices.get(position).getName());
    BluetoothDevice device = devices.get(position);
    holder.deviceNameTextView.setOnClickListener(v->onDeviceClickListener.onDeviceClick(device,v.getRootView()));

}

@Override
public int getItemCount() {
    return devices.size();
}

static class DeviceViewHolder extends RecyclerView.ViewHolder {
    TextView deviceNameTextView;
    TextView paringText;
    boolean isParing = false;

    public DeviceViewHolder(@NonNull View itemView) {
        super(itemView);
        deviceNameTextView = itemView.findViewById(R.id.deviceName);
        paringText = itemView.findViewById(R.id.isParing);
    }
}
}
