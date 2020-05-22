package com.example.mcs

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception
import java.util.*


private const val SCAN_PERIOD: Long = 10000

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MainActivity : AppCompatActivity(), SensorEventListener {

    var sensorManager: SensorManager? = null;
    var accSensor: Sensor? = null
    var captureXdata = true
    var startXTime: Long? = null
    private val maxVertical = 3.0
    private var mStartTime: Long = 0
    private var isCounterUp = false
    private val SHAKE_THRESHOLD = 800
    private var last_x: Float = 0f
    private var last_y: Float = 0f
    private var last_z: Float = 0f
    private var lastUpdate: Long = 0
    private var REQUEST_ENABLE_BT = 1

    private var advertiser: BluetoothLeAdvertiser? = null

    private var currentAdvertisingSet: AdvertisingSet? = null

    private var advParams: AdvertisingSetParameters? = null;

    private var advData: AdvertiseData? = null;

    private var advCallback: AdvertisingSetCallback? = null;

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // private var scanActivity: DeviceScanActivity? = null;

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        bluetoothAdapter.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // scanActivity = DeviceScanActivity(bluetoothAdapter, this);

        if(!bluetoothAdapter.isDisabled) {
          //  scanActivity!!.scanLeDevice(true);
            advertiseSetup()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun advertiseSetup() {
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser

        advCallback = @RequiresApi(Build.VERSION_CODES.O)
        object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet,
                txPower: Int,
                status: Int
            ) {
                Log.i(
                    "LOG", "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                            + status
                )
                currentAdvertisingSet = advertisingSet
            }

            override fun onAdvertisingDataSet(
                advertisingSet: AdvertisingSet,
                status: Int
            ) {
                Log.i("LOG", "onAdvertisingDataSet() :status:$status")
            }

            override fun onScanResponseDataSet(
                advertisingSet: AdvertisingSet,
                status: Int
            ) {
                Log.i("LOG", "onScanResponseDataSet(): status:$status")
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
                Log.i("LOG", "onAdvertisingSetStopped():")
            }
        }

        advParams = (AdvertisingSetParameters.Builder())
            .setLegacyMode(true)
            .setConnectable(true)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .build()

    }



    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // TODO
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {

        if(event != null) {

            if(startXTime != null && System.currentTimeMillis() - startXTime!! >  1500 && !captureXdata) {
                captureXdata = true;
                // Log.d("PODE:", "já pode voltar a capturar X")
            }

            if(isPhoneVertical(event)) {
                if(!isCounterUp) {
                    mStartTime = getStartTime();
                    isCounterUp = true;
                } else {
                    if(getDiffTime() > 1500) {
                        // Log.d("Y", "Anterior");
                        isCounterUp = false;
                        // ENVIA ANTERIOR
                    }
                }
            }

            if(captureXdata) {
                if(event.values[0] > 2) {
                    captureXdata = false;
                    startXTime = System.currentTimeMillis();
                    //Log.d("X", "Direita " + event.values[0].toString())
                    if(event.values[0] > 4) {
                        // ENVIAR 20%+
                    } else {
                        // ENVIAR 10%+
                        val byteStream: ByteArray = byteArrayOf(0x1, 0x2, 0x3);

                        advData = AdvertiseData.Builder().setIncludeDeviceName(true).addServiceData(
                            ParcelUuid(UUID.randomUUID()), byteStream ).build()

                        advertiser?.startAdvertisingSet(advParams, advData, null, null, null, advCallback);                        // ENVIAR 10%+
                    }

                } else if (event.values[0] < -2) {
                    captureXdata = false;
                    startXTime = System.currentTimeMillis();
                    // Log.d("X", "Esquerda " + event.values[0].toString())

                    if(event.values[0] < -4) {
                        // ENVIAR 20%-
                    } else {
                        // ENVIAR 10%-
                    }
                }
            }

            val curTime = System.currentTimeMillis()
            // only allow one update every 100ms.
            // only allow one update every 100ms.
            if (curTime - lastUpdate > 100) {

                val diffTime: Long = curTime - lastUpdate
                lastUpdate = curTime

                var x: Float = event.values[0]
                var y: Float = event.values[1]
                var z: Float = event.values[2]

                val speed: Float =
                    Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000
                if (speed > SHAKE_THRESHOLD) {
                    // PRÓXIMA
                   // Log.d("XYZ", "PROXIMA")
                }
                last_x = x
                last_y = y
                last_z = z
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accSensor?.also {
            l ->
            sensorManager?.registerListener(this, l, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    // This method return true only for specific phone orientation
// y axis for vertical orientation
    private fun isPhoneVertical(event: SensorEvent): Boolean {
        val values = event.values
        val y = values[1].toDouble()
        // do not change this value
        val yAxisInitValue = 1.5
        val verMargin: Double = yAxisInitValue - maxVertical
        return y >= verMargin
    }

    private fun getStartTime(): Long {
        return System.currentTimeMillis();
    }

    private fun getDiffTime(): Long {
        return getStartTime() - mStartTime
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled


}
