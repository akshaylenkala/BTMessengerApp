package com.example.androidbluetoothchat;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

/** @noinspection ALL*/
public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE_ADDRESS = "device_address" ;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ChatUtils chatUtils;
    private final int PICK_FILE_REQUEST_CODE = 124;
    private Uri selectedFileUri;
    private ListView listMainChat;
    private EditText edCreateMessage;
    ImageButton btnSendMessage;
    private ArrayAdapter<String> adapterMainChat;
    private final int LOCATION_PERMISSION_REQUEST = 101;
    private final int SELECT_DEVICE = 102;
    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;
    private ImageButton btnSelectImage;
    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private String connectedDevice;
    private String connectedDeviceAddress;
    private MediaPlayer mediaPlayer;
    private MediaPlayer rmediaplayer;
    private MediaPlayer pairedsound;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_READ:
                    MessageData messageData = (MessageData) message.obj;
                    byte[] buffer = messageData.getMessageBytes();
                    String timestamp = messageData.getTimestamp();
                    String senderReceiver=messageData.getSenderReceiver();
                    processReceivedMessage(buffer,senderReceiver,timestamp);
                    break;
                case MESSAGE_STATE_CHANGED:
                    switch (message.arg1) {
                        case ChatUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Listening");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);
                            playDeviceConnectedSound();
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) message.obj;
                    String outputBuffer = new String(buffer1);
                    adapterMainChat.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });
    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer = MediaPlayer.create(this, R.raw.sent_sound);
        rmediaplayer = MediaPlayer.create(this, R.raw.rec_sound);
        pairedsound = MediaPlayer.create(this, R.raw.paired);
        context = this;
        init();
        initBluetooth();
        chatUtils = new ChatUtils(context, handler);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }
    private void init() {
        listMainChat = findViewById(R.id.list_conversation);
        edCreateMessage = findViewById(R.id.ed_enter_message);
        btnSendMessage = (ImageButton) findViewById(R.id.btn_send_msg);
        btnSelectImage = findViewById(R.id.btn_select_image);
        adapterMainChat = new ArrayAdapter<String>(context, R.layout.message_layout);
        listMainChat.setAdapter(adapterMainChat);
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFilePicker();
            }
        });
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = edCreateMessage.getText().toString();
                if (!message.isEmpty()) {
                    if(!connectedDevice.isEmpty())
                    {mediaPlayer.start();}
                    edCreateMessage.setText("");
                    chatUtils.write(message.getBytes());
                } else if (selectedFileUri != null) {
                    sendSelectedFile(selectedFileUri);
                    selectedFileUri = null;
                } else {
                    Toast.makeText(context, "Please enter a message or select a file", Toast.LENGTH_SHORT).show();
                }

                int k=listMainChat.getMaxScrollAmount();
                listMainChat.smoothScrollToPosition(k+13);
            }
        });
    }
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }
    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "No Bluetooth Found", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_devices:
                checkPermissions();
                return true;
            case R.id.menu_enable_bluetooth:
                enableBluetooth();
                return true;
            case R.id.action_guidelines:
                openGuidelinesActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void openGuidelinesActivity() {
        Intent intent = new Intent(MainActivity.this, GuidelinesActivity.class);
        startActivity(intent);
    }
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            Intent intent = new Intent(context, DeviceListActivity.class);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, connectedDeviceAddress);
            startActivityForResult(intent, SELECT_DEVICE);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            initiateBluetoothFileTransfer(selectedFileUri);
            sendSelectedFile(selectedFileUri);
        } else if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            String address = data.getStringExtra("deviceAddress");
            connectedDeviceAddress = address;
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
    }
    private void sendSelectedFile(Uri selectedFileUri) {
        if (chatUtils != null && chatUtils.isConnected()) {
        } else {
            Toast.makeText(context, "Not connected or connection not yet established", Toast.LENGTH_SHORT).show();
        }
    }
    private void initiateBluetoothFileTransfer(Uri fileUri) {
        if (connectedDeviceAddress != null && !connectedDeviceAddress.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setPackage("com.android.bluetooth");
            intent.putExtra("android.bluetooth.device.extra.NAME", connectedDevice);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(context, DeviceListActivity.class);
                startActivityForResult(intent, SELECT_DEVICE);
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\n Please grant")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                checkPermissions();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.enable();
        }
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoveryIntent);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatUtils != null) {
            chatUtils.stop();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (rmediaplayer != null) {
            rmediaplayer.release();
            rmediaplayer = null;
        }
        if (pairedsound != null) {
            pairedsound.release();
            pairedsound = null;
        }
    }
    private void processReceivedMessage(byte[] buffer, String senderReceiver, String timestamp) {
        String receivedMessage = new String(buffer);
       if (!receivedMessage.equals("<FILE>")) {
            if(!senderReceiver.equals("Me")){playReceivedMessageSound();}
            String formattedMessage = senderReceiver + ": "+receivedMessage+"\n"+timestamp;
            adapterMainChat.add(formattedMessage);
       }
        listMainChat.smoothScrollToPosition(adapterMainChat.getCount() - 1);
    }
    private void playReceivedMessageSound() {
        if (rmediaplayer != null) {
            try {
                rmediaplayer.stop();
                rmediaplayer.prepare(); // Prepare MediaPlayer for playback (reset state)
                rmediaplayer.start(); // Start playback
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void playDeviceConnectedSound() {
        if (pairedsound != null) {
            try {
                pairedsound.stop();
                pairedsound.prepare(); // Prepare MediaPlayer for playback (reset state)
                pairedsound.start(); // Start playback
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
}