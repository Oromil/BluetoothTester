package com.example.shakin.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String SAVED_DEVICE = "saved_device";

    private BluetoothAdapter mAdapter;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mCharacteristic;

    private ProgressBar mProgress;
    private ViewGroup mLayout;

    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreferences = this.getPreferences(MODE_PRIVATE);


        final EditText etCommand = findViewById(R.id.editText);

        mLayout = findViewById(R.id.layout);
        mProgress = findViewById(R.id.progress);

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mPreferences.getString(SAVED_DEVICE, "").equals("")) {
            new MaterialDialog.Builder(this)
                    .title(R.string.title_dialog)
                    .content(R.string.text_dialog)
                    .positiveText(R.string.ok)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .cancelable(false)
                    .input("", "", false, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            mPreferences.edit().putString(SAVED_DEVICE, input.toString()).apply();
                            checkBluetoothEnable();
                        }
                    }).show();
        } else {

            checkBluetoothEnable();
        }


        findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = etCommand.getText().toString();

                int len = command.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    data[i / 2] = (byte) ((Character.digit(command.charAt(i), 16) << 4)
                            + Character.digit(command.charAt(i + 1), 16));
                }
                mCharacteristic.setValue(data);
                mGatt.writeCharacteristic(mCharacteristic);
            }
        });
    }

    private void showLoading(boolean show) {
        if (show) {
            mProgress.setVisibility(View.VISIBLE);
            mLayout.setVisibility(View.GONE);
        } else {
            mProgress.setVisibility(View.GONE);
            mLayout.setVisibility(View.VISIBLE);
        }
    }

    private void checkBluetoothEnable() {
        if (isBluetoothEnable()) {
            connect();
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            requestBluetoothEnable(enableBtIntent);
        }
    }

    private boolean isBluetoothEnable() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return !(bluetoothAdapter == null || !bluetoothAdapter.isEnabled());
    }

    public void requestBluetoothEnable(Intent intent) {
        startActivityForResult(intent, 5);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 5) {
            if (resultCode == RESULT_OK)
                connect();
            else requestBluetoothEnable(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
    }

    private void connect() {

        mGatt = mAdapter.getRemoteDevice(mPreferences.getString(SAVED_DEVICE, "")).connectGatt(this, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                mCharacteristic = gatt.getService(UUID.fromString("1d5688de-866d-3aa4-ec46-a1bddb37ecf6"))
                        .getCharacteristic(UUID.fromString("af20fbac-2518-4998-9af7-af42540731b3"));

                mGatt.setCharacteristicNotification(mCharacteristic, true);

                BluetoothGattDescriptor desc = mCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                gatt.writeDescriptor(desc);

            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);

                showLoading(false);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        });
    }
}
