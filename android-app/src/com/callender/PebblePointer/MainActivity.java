package com.callender.PebblePointer;

import java.util.Set;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends ListActivity {
	
	private BluetoothAdapter mBtAdapter;
    public static String DEVICE_MAC_ADDRESS;
    public static Set<BluetoothDevice> pairedDevices;
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	// check BT state in case something was changed while app is paused
    	checkBTState();
    	// Get local BT adapter
    	mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    	// Find currently paired devices
    	pairedDevices = mBtAdapter.getBondedDevices();
    }
    
    //Checks if BT is on or off
    //Prompts the user to turn it on if it is off
    private void checkBTState()
    {
        // Check device has Bluetooth and that it is turned on
        mBtAdapter=BluetoothAdapter.getDefaultAdapter(); // CHECK THIS OUT THAT IT WORKS!!!
        if(mBtAdapter==null) {
               Toast.makeText(getBaseContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
               finish();
        } else {
          if (!mBtAdapter.isEnabled()) {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
            }
          }
     }
    
	/*
	 * Dialog shown when choosing a device to pair
	 */
	void showDialog() {
    	DialogFragment newFragment = PairedDevicesFragment.newInstance();
        newFragment.show(getFragmentManager(), "dialog");
    }
	
	/*
	 * Attempt to connect to BT on the next Activity
	 */
	public void connectBluetooth() {
		Intent i = new Intent(this, AccelerometerActivity.class);
		i.putExtra("MACAddress", DEVICE_MAC_ADDRESS);
		startActivity(i);
	}

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] menu = {
                "Raw Accelerometer Vectors",
        };

        setListAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_1, menu));
    }

    @Override
    protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
        switch (position) {
            case 0:
            	showDialog();
                //intent = new Intent(this, AccelerometerActivity.class);
                break;               
        }
    }

}
