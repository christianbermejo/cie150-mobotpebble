package com.callender.PebblePointer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class PairedDevicesFragment extends DialogFragment {
	
	// ArrayAdapter for paired devices
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    public static PairedDevicesFragment newInstance() {
    	/*
    	 * Set up Fragment
    	 */
        PairedDevicesFragment frag = new PairedDevicesFragment();
        return frag;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	/*
    	 * Inflate Fragment
    	 */
    	LayoutInflater i = getActivity().getLayoutInflater();
    	View view = i.inflate(R.layout.popup, null);
    	
    	mPairedDevicesArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.device_name);
    	ListView lv = (ListView) view.findViewById(R.id.paired_devices);
    	lv.setAdapter(mPairedDevicesArrayAdapter);
    	
        // Add previously paired devices to the array
        if (MainActivity.pairedDevices.size() > 0) {
            for (BluetoothDevice device : MainActivity.pairedDevices) {
            	Log.i("array add", device.getName().toString() + ", " + device.getAddress().toString());
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            mPairedDevicesArrayAdapter.add("No devices paired");
        }
        
    	lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// Get MAC address which is the last 17 characters
				String info = ((TextView) view).getText().toString();
				if (info.equalsIgnoreCase("No devices paired")) {
					// Nothing should be done
				} else {
	            MainActivity.DEVICE_MAC_ADDRESS = info.substring(info.length() - 17);
	            ((MainActivity)getActivity()).connectBluetooth();
	            // Dismiss fragment
	            dismiss();
				}
			}
		});
        return new AlertDialog.Builder(getActivity())
		.setTitle("Choose paired device to connect")
		.setView(view)
        .create();
    }
}
