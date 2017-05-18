package com.bluebot;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    final String FORWARD = "f";
    final String BACKWARD = "b";
    final String LEFT = "l";
    final String RIGHT = "r";
    final String STOP = "s";
    final String LEFTBACK = "L";
    final String RIGHTBACK = "R";
    final String AUTOPILOT = "a";

    boolean forward = false;
    boolean backward = false;
    boolean left = false;
    boolean right = false;
    boolean auto = false;
    boolean stop = false;

    BluetoothSPP bluetooth;
    Button connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetooth = new BluetoothSPP(this);
        connect = (Button) findViewById(R.id.connect);

        if (!bluetooth.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            //close the Activity
            finish();
        }
        setBluetoothListener();
        setConnectListener();
        setButtonsOnTouchListener();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetooth.stopService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!bluetooth.isBluetoothEnabled()) {
            bluetooth.enable();
        } else {
            if (!bluetooth.isServiceAvailable()) {
                bluetooth.setupService();
                bluetooth.startService(BluetoothState.DEVICE_OTHER);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                connect.setText("Connecting...");
                connect.setEnabled(false);
                bluetooth.connect(data);
            }
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bluetooth.setupService();
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth was not enabled.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Set buttons on touch listener
     */
    private void setButtonsOnTouchListener() {
        findViewById(R.id.forward).setOnTouchListener(this);
        findViewById(R.id.backward).setOnTouchListener(this);
        findViewById(R.id.left).setOnTouchListener(this);
        findViewById(R.id.right).setOnTouchListener(this);
        findViewById(R.id.auto).setOnTouchListener(this);
    }

    /**
     * Set connection to phone listener
     */
    private void setConnectListener() {
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetooth.getServiceState() == BluetoothState.STATE_CONNECTED) {
                    bluetooth.disconnect();
                } else {
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                }
            }
        });
    }

    /**
     * Set Bluetooth Listener
     */
    private void setBluetoothListener() {
        bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                connect.setEnabled(true);
                connect.setText("Connected to " + name);
            }

            @Override
            public void onDeviceDisconnected() {
                connect.setEnabled(true);
                connect.setText("Connection lost");
            }

            @Override
            public void onDeviceConnectionFailed() {
                connect.setEnabled(true);
                connect.setText("Unable to connect");
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.forward:
                if (event.getAction() == MotionEvent.ACTION_DOWN) forward = true;
                else if (event.getAction() == MotionEvent.ACTION_UP) forward = false;
                break;
            case R.id.backward:
                if (event.getAction() == MotionEvent.ACTION_DOWN) backward = true;
                else if (event.getAction() == MotionEvent.ACTION_UP) backward = false;
                break;
            case R.id.left:
                if (event.getAction() == MotionEvent.ACTION_DOWN) left = true;
                else if (event.getAction() == MotionEvent.ACTION_UP) left = false;
                break;
            case R.id.right:
                if (event.getAction() == MotionEvent.ACTION_DOWN) right = true;
                else if (event.getAction() == MotionEvent.ACTION_UP) right = false;
                break;
            case R.id.auto:
                if (event.getAction() == MotionEvent.ACTION_DOWN) auto = true;
                else if (event.getAction() == MotionEvent.ACTION_UP) auto = false;
                break;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP)
            command();

        return false;
    }

    private void command() {
        if (stop) {
            send(STOP);
        } else if (forward) {
            if (left && !right) send(LEFT);
            else if (right) send(RIGHT);
            else if (!backward) send(FORWARD);
            else send(STOP);
        } else if (backward) {
            if (left && !right) send(LEFTBACK);
            else if (right) send(RIGHTBACK);
            else if (!forward) send(BACKWARD);
            else send(STOP);
        } else if (left && !right) send(LEFT);
        else if (right) send(RIGHT);
        else if(auto)send(AUTOPILOT);
        else send(STOP);

    }

    private void send(String command) {
        bluetooth.send(command,true);
    }
}
