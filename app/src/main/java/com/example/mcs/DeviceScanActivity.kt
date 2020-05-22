package com.example.mcs

import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi

private const val SCAN_PERIOD: Long = 10000
private const val START_GATT_DELAY = 500
/**
 * Activity for scanning and displaying available BLE devices.
 */
class DeviceScanActivity(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val context: Context
)  {

    private var mScanning: Boolean = false

    val listDevices: MutableList<BluetoothDevice> = mutableListOf();

    private var bluetoothGatt: BluetoothGatt? = null

    var currentDevice: BluetoothDevice? = null;

    var isConnected: Boolean = false;

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                mScanning = true
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.startLeScan(leScanCallback)
                }
            }
            else -> {
                mScanning = false
                if (bluetoothAdapter != null) {
                    bluetoothAdapter.stopLeScan(leScanCallback)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private val leScanCallback = BluetoothAdapter.LeScanCallback {
        device, rssi, scanRecord ->
            if(device != null) {
                addDevice(device);
            }
    }
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun addDevice(device: BluetoothDevice) {
        // #TODO: mehorar este if para se conectar ao device pretendido e não ao primeiro
        if(!listDevices.contains(device) && listDevices.count() < 1) {
            listDevices.add(device);

            // #TODO: escolher ao qual conectar aqui
            currentDevice = device;

            bluetoothGatt = device.connectGatt(context, false, gattCalback);

        }
    }

    private val gattCalback = @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if(newState === 0) {
                 Log.d("BLE_NEWSTATUS", "DISCONNECTED");
            } else if (newState === 1) {
                 Log.d("BLE_NEWSTATUS","CONNECTING");
            } else if(newState === 2) {
                Log.d("BLE_NEWSTATUS", "CONNECTED")
            }

            if(status === 0) {
                Log.d("BLE_STATUS", "SUCCESS")
                isConnected = true;
                // Significa que se conectou corretamente
            } else {
                isConnected = false;
            }

        }
    }
}