package com.example.mcs

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.*


private const val SCAN_PERIOD: Long = 10000
private const val START_GATT_DELAY = 500
private const val BLE_BUNDLE_MACADDRESS = "18:E8:29:4A:A6:B0";

private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2
const val ACTION_GATT_CONNECTED = "com.example.mcs.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.example.mcs.ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED =
    "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_DATA_AVAILABLE = "com.example.mcs.ACTION_DATA_AVAILABLE"
const val EXTRA_DATA = "com.example.mcs.EXTRA_DATA"
val UUID_HEART_RATE_MEASUREMENT = UUID.fromString("4de0d2dc-9dd9-11ea-bb37-0242ac130002")

/**
 * Activity for scanning and displaying available BLE devices.
 */
class DeviceScanActivity(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val context: Context
)  {

    private var mScanning: Boolean = false

    // val listDevices: MutableList<BluetoothDevice> = mutableListOf();

    var currentDevice: BluetoothDevice? = null;

    var isConnected: Boolean = false;

    var bluetoothGatt: BluetoothGatt? = null;

    var hasDiscoveredSrv: Boolean = false;

    private var connectionState = STATE_DISCONNECTED

    var char: BluetoothGattCharacteristic? = null;

    var sensorManager: SensorManager? = null;
    var accSensor: Sensor? = null

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
            if(device != null && device.address == BLE_BUNDLE_MACADDRESS && currentDevice == null) {
                scanLeDevice(false);
                addDevice(device);
            }
    }
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun addDevice(device: BluetoothDevice) {
         currentDevice = device;

        bluetoothGatt = device.connectGatt(context, false, gattCalback);
        scanLeDevice(false);

        Log.d("SENSOR_CHANGED","ADICIONA_DEVICE")
        Log.d("SENSOR_CHANGED",device.address)

    }

    private val gattCalback = @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)


            if(newState === 0) {
                 Log.d("BLE_NEWSTATUS", "DISCONNECTED");
                isConnected = false;

                Log.i("BLE_NEWSTATUS", "Disconnected from GATT server.")

            } else if (newState === 1) {
                 Log.d("BLE_NEWSTATUS","CONNECTING");
                isConnected = false;

            } else if(newState === BluetoothProfile.STATE_CONNECTED) {

                Log.d("BLE_NEWSTATUS", "CONNECTED")
                isConnected = true;



            hasDiscoveredSrv = bluetoothGatt?.discoverServices()!!;
                Log.i("BLE_NEWSTATUS", "Connected to GATT server.")
                Log.i("BLE_NEWSTATUS", "Attempting to start service discovery: " +
                        hasDiscoveredSrv)


            }

            if(status === 0) {
                Log.d("BLE_STATUS", "SUCCESS")
                isConnected = true;
                Log.d("SENSOR_CHANGED","CONECTA_SE")

                // Significa que se conectou corretamente
            } else {
                isConnected = false;
            }

        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            var srv: BluetoothGattService? = null;

            srv = bluetoothGatt?.getService(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"));
            Log.d("SENSOR_CHANGED","DESCOBRE_SERVIÇO")

            if(srv != null && char == null) {
                char = srv.getCharacteristic(UUID.fromString(("00002a00-0000-1000-8000-00805f9b34fb")))
                Log.d("SENSOR_CHANGED","DESCOBRE_CHARACTERISTICAS")

            }
        }


    }


}



