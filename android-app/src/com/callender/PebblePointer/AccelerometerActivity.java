package com.callender.PebblePointer;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;


/**
 *  Receive accelerometer vectors from Pebble watch via PebblePointer app.
 *
 *  @author robin.callender@gmail.com
 *  @author Christian Bermejo
 */
public class AccelerometerActivity extends Activity {

    private static final String TAG = "PebblePointer";
    
    // To be used by the BT adapter
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    
    // Standard UUID of HC-05 BT module
    private static final UUID HC05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
   
    // MAC-address of BT module
    public String newAddress = null;

    // The tuple key corresponding to a vector received from the watch
    private static final int PP_KEY_CMD = 128;
    private static final int PP_KEY_X   = 1;
    private static final int PP_KEY_Y   = 2;
    private static final int PP_KEY_Z   = 3;

    @SuppressWarnings("unused")
    private static final int PP_CMD_INVALID = 0;
    private static final int PP_CMD_VECTOR  = 1;

    public static final int VECTOR_INDEX_X  = 0;
    public static final int VECTOR_INDEX_Y  = 1;
    public static final int VECTOR_INDEX_Z  = 2;

    private static int vector[] = new int[3];

    private PebbleKit.PebbleDataReceiver dataReceiver;

    // This UUID identifies the PebblePointer app.
    private static final UUID PEBBLEPOINTER_UUID = UUID.fromString("273761eb-97dc-4f08-b353-3384a2170902");

    private static final int SAMPLE_SIZE = 30;

    private XYPlot dynamicPlot = null;

    SimpleXYSeries xSeries = null;
    SimpleXYSeries ySeries = null;
    SimpleXYSeries zSeries = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate: ");

        setContentView(R.layout.activity_accelerometer);
        
        // Initialize BT adapter and check state
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        vector[VECTOR_INDEX_X] = 0;
        vector[VECTOR_INDEX_Y] = 0;
        vector[VECTOR_INDEX_Z] = 0;

        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLEPOINTER_UUID);


        dynamicPlot = (XYPlot) findViewById(R.id.dynamicPlot);


        dynamicPlot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
        dynamicPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);

        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0.0"));
        dynamicPlot.getGraphWidget().setRangeValueFormat(new DecimalFormat("0"));

        dynamicPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);

        dynamicPlot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
        dynamicPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);

        dynamicPlot.setTicksPerDomainLabel(1);
        dynamicPlot.setTicksPerRangeLabel(1);

        dynamicPlot.getGraphWidget().getDomainLabelPaint().setTextSize(30);
        dynamicPlot.getGraphWidget().getRangeLabelPaint().setTextSize(30);

        dynamicPlot.getGraphWidget().setDomainLabelWidth(40);
        dynamicPlot.getGraphWidget().setRangeLabelWidth(80);

        dynamicPlot.setDomainLabel("time");
        dynamicPlot.getDomainLabelWidget().pack();

        dynamicPlot.setRangeLabel("G-force");
        dynamicPlot.getRangeLabelWidget().pack();

        dynamicPlot.setRangeBoundaries(-1024, 1024, BoundaryMode.FIXED);
        dynamicPlot.setDomainBoundaries(0, SAMPLE_SIZE, BoundaryMode.FIXED);


        xSeries = new SimpleXYSeries("X-axis");
        xSeries.useImplicitXVals();

        ySeries = new SimpleXYSeries("Y-axis");
        ySeries.useImplicitXVals();

        zSeries = new SimpleXYSeries("Z-axis");
        zSeries.useImplicitXVals();

        // Blue line for X axis.
        LineAndPointFormatter fmtX = new LineAndPointFormatter(Color.BLUE, null, null, null);
        dynamicPlot.addSeries(xSeries, fmtX);

        // Green line for Y axis.
        LineAndPointFormatter fmtY = new LineAndPointFormatter(Color.GREEN, null, null, null);;
        dynamicPlot.addSeries(ySeries, fmtY);

        // Red line for Z axis.
        LineAndPointFormatter fmtZ = new LineAndPointFormatter(Color.RED, null, null, null);
        dynamicPlot.addSeries(zSeries, fmtZ);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG, "onPause: ");
        
        //Pausing can be the end of an app if the device kills it or the user doesn't open it again
        //close all connections so resources are not wasted
     
        //Close BT socket to device
        try     {
          btSocket.close();
        } catch (IOException e2) {
            Toast.makeText(getBaseContext(), "ERROR - Failed to close Bluetooth socket", Toast.LENGTH_SHORT).show();
        }

        setContentView(R.layout.activity_accelerometer);

        if (dataReceiver != null) {
                unregisterReceiver(dataReceiver);
                dataReceiver = null;
        }
        
        PebbleKit.closeAppOnPebble(getApplicationContext(), PEBBLEPOINTER_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "onResume: ");

        final Handler handler = new Handler();

        dataReceiver = new PebbleKit.PebbleDataReceiver(PEBBLEPOINTER_UUID) {

            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary dict) {

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        PebbleKit.sendAckToPebble(context, transactionId);

                        final Long cmdValue = dict.getInteger(PP_KEY_CMD);
                        if (cmdValue == null) {
                            return;
                        }

                        if (cmdValue.intValue() == PP_CMD_VECTOR) {

                            // Capture the received vector.
                            final Long xValue = dict.getInteger(PP_KEY_X);
                            if (xValue != null) {
                                vector[VECTOR_INDEX_X] = xValue.intValue();
                            }

                            final Long yValue = dict.getInteger(PP_KEY_Y);
                            if (yValue != null) {
                                vector[VECTOR_INDEX_Y] = yValue.intValue();
                            }

                            final Long zValue = dict.getInteger(PP_KEY_Z);
                            if (zValue != null) {
                                vector[VECTOR_INDEX_Z] = zValue.intValue();
                            }
                            
                            // Send the values to Bluetooth
                            String x = String.valueOf(xValue.intValue()) + "\n";
                            String y = String.valueOf(yValue.intValue()) + "\n";
                            String z = String.valueOf(zValue.intValue()) +  "\n";
                            sendData(stringToBytesUTFCustom(x+y+z));
                            // Update the user interface.
                            updateUI();
                        }
                    }
                });
            }
        };
        
     // connection methods are best here in case program goes into the background etc
        
        //Get MAC address from DeviceListActivity
        Intent intent = getIntent();
        // MainActivity.DEVICE_MAC_ADDRESS
        newAddress = intent.getStringExtra("MACAddress");
  
        // Set up a pointer to the remote device using its address.
        BluetoothDevice device = btAdapter.getRemoteDevice(newAddress);
     
        //Attempt to create a bluetooth socket for comms
        try {
            btSocket = device.createRfcommSocketToServiceRecord(HC05_UUID);
        } catch (IOException e1) {
            Toast.makeText(getBaseContext(), "ERROR - Could not create Bluetooth socket", Toast.LENGTH_SHORT).show();
        }
     
        // Establish the connection.
        try {
          btSocket.connect();
        } catch (IOException e) {
          try {
            btSocket.close();        //If IO exception occurs attempt to close socket
          } catch (IOException e2) {
              Toast.makeText(getBaseContext(), "ERROR - Could not close Bluetooth socket", Toast.LENGTH_SHORT).show();
          }
        }
     
        // Create a data stream so we can talk to the device
        try {
          outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "ERROR - Could not create bluetooth outstream", Toast.LENGTH_SHORT).show();
        }
        //When activity is resumed, attempt to send a piece of junk data ('x') so that it will fail if not connected
        // i.e don't wait for a user to press button to recognise connection failure
        
        PebbleKit.registerReceivedDataHandler(this, dataReceiver);
    }
    
 // Method to send data
    private void sendData(byte[] s) {
      try {
      //attempt to place data on the outstream to the BT device
        outStream.write(s);
      } catch (IOException e) {
         //if the sending fails this is most likely because device is no longer there
         Toast.makeText(getBaseContext(), "ERROR - Device not found", Toast.LENGTH_SHORT).show();
         finish();
      }
    }
    
    // static
    // Convert string to byte array for Arduino
    public byte[] stringToBytesUTFCustom(String str) {
    		 byte[] b = new byte[str.length() << 1];
    		 for(int i = 0; i < str.length(); i++) {
    		  char strChar = str.charAt(i);
    		  int bpos = i << 1;
    		  b[bpos] = (byte) ((strChar&0xFF00)>>8);
    		  b[bpos + 1] = (byte) (strChar&0x00FF);
    		 }
    		 return b;
    		}
    
    
    
    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "ERROR - Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else {
          if (btAdapter.isEnabled()) {
          } else {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
          }
        }
      }

    public void updateUI() {

        final String x = String.format(Locale.getDefault(), "X: %d", vector[VECTOR_INDEX_X]);
        final String y = String.format(Locale.getDefault(), "Y: %d", vector[VECTOR_INDEX_Y]);
        final String z = String.format(Locale.getDefault(), "Z: %d", vector[VECTOR_INDEX_Z]);

        // Update the numerical fields

        TextView x_axis_tv = (TextView) findViewById(R.id.x_axis_Text);
        x_axis_tv.setText(x);

        TextView y_axis_tv = (TextView) findViewById(R.id.y_axis_Text);
        y_axis_tv.setText(y);

        TextView z_axis_tv = (TextView) findViewById(R.id.z_axis_Text);
        z_axis_tv.setText(z);

        // Update the Plot

        // Remove oldest vector data.
        if (xSeries.size() > SAMPLE_SIZE) {
            xSeries.removeFirst();
            ySeries.removeFirst();
            zSeries.removeFirst();
        }

        // Add the latest vector data.
        xSeries.addLast(null, vector[VECTOR_INDEX_X]);
        ySeries.addLast(null, vector[VECTOR_INDEX_Y]);
        zSeries.addLast(null, vector[VECTOR_INDEX_Z]);

        // Redraw the Plots.
        dynamicPlot.redraw();
    }

}