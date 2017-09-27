package com.krajinov.bluetoothled;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int POWER_ON_BT = 111;
    public static final String EXTRA_ADDRESS = "MAC_Address";

    private BluetoothAdapter mBluetoothAdapter = null;
    private Set<BluetoothDevice> pairedDevices;
    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver mReceiver;
    private ArrayList<String> listOfDevices;
    private ArrayAdapter<String> arrayAdapter;

    @BindView(R.id.listDev)
    ListView mListDev;
    @BindView(R.id.progress_holder)
    LinearLayout pbHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initBluetooth();
    }

    private void initBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        } else if (mBluetoothAdapter.isEnabled()) {
            showPairedDevicesList();
        } else {
            //Ask the user to turn the bluetooth on
            Intent powerOnBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(powerOnBT, POWER_ON_BT);
        }
    }

    @OnClick(R.id.btnShwDev)
    public void showPairedDevicesList() {
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        listOfDevices = new ArrayList();

        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice bt : pairedDevices) {
                listOfDevices.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        } else {
            Toast.makeText(this, "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }
        arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, listOfDevices);
        mListDev.setAdapter(arrayAdapter);
        mListDev.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the device MAC address, the last 17 chars in the View
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                // Make an intent to start next activity.
                Intent i = new Intent(MainActivity.this, LedControlActivity.class);
                //Change the activity.
                i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
                startActivity(i);
            }
        });
    }

    @OnClick(R.id.btnScan)
    public void scanBTDevices() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
        showLoading(true);
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Finding devices
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    showLoading(false);
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceItem = device.getName() + "\n" + device.getAddress();
                    if (listOfDevices == null) {
                        listOfDevices = new ArrayList<>();
                    }
                    for (String s : listOfDevices) {
                        if (s.equalsIgnoreCase(deviceItem)) {
                            return;
                        }
                    }
                    // Add the name and address to an array adapter to show in a ListView
                    listOfDevices.add(deviceItem);
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        broadcastManager.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (broadcastManager != null) {
            broadcastManager.unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("BluetoothResult", ": " + resultCode);
        if (requestCode == POWER_ON_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Can't proceed! Bluetooth is disabled!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void showLoading(boolean loading) {
        pbHolder.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}