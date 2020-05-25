package com.example.mcs

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


private const val SCAN_PERIOD: Long = 10000

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MainActivity : AppCompatActivity(), SensorEventListener {

    var sensorManager: SensorManager? = null;
    var accSensor: Sensor? = null
    var captureXdata = true
    var startXTime: Long? = null
    private var mStartTime: Long = 0
    private var isCounterUp = false
    private val SHAKE_THRESHOLD = 800
    private var last_x: Float = 0f
    private var last_y: Float = 0f
    private var last_z: Float = 0f
    private var lastUpdate: Long = 0
    private var REQUEST_ENABLE_BT = 1

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var scanActivity: DeviceScanActivity? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

       /* bluetoothAdapter.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } */


        // scanActivity = DeviceScanActivity(bluetoothAdapter, this);

        if(!bluetoothAdapter.isDisabled) {
            // scanActivity!!.scanLeDevice(true);
        }
    }


private fun sendData(data: String) {
   /* scanActivity?.char!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
    scanActivity?.char!!.setValue(data.toByteArray())
    scanActivity?.bluetoothGatt?.writeCharacteristic(scanActivity?.char); */
    Log.d("COMMAND_", data)

   val url = "http://4a3f8692.ngrok.io/?command=$data";
//val url = "https://ooleklelele1.requestcatcher.com/test"
    AsyncTaskHandleJson().execute(url)


  /* with(url.openConnection() as HttpURLConnection) {
        requestMethod = "GET"

        Log.d("GET_CONNECTION", "CONNECTION_DONE")
    }*/

}

    inner class AsyncTaskHandleJson: AsyncTask<String, String, String>() {
        override fun doInBackground(vararg params: String?): String {
            var text: String = ""
            val conn = URL(params[0]).openConnection() as HttpURLConnection

            try {
                conn.connect()
                text = conn.inputStream.use {
                    it.reader().use{reader -> reader.readText()}
                }
            } catch (e: Exception) {
                Log.d("COMMAND_", e.toString())
            }
            /*finally {
                conn.disconnect()
            }*/

            Log.d("COMMAND_", conn.responseCode.toString());
            Log.d("COMMAND_", conn.responseMessage)
            return text;

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // TODO
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event != null) {
            if(startXTime != null && System.currentTimeMillis() - startXTime!! >  1500 && !captureXdata) {
                captureXdata = true;
            }

            if(isPhoneVertical(event)) {
                if(!isCounterUp) {
                    mStartTime = getStartTime();
                    isCounterUp = true;
                } else {
                    if(getDiffTime() > 10000) {
                        // Log.d("Y", "Anterior");
                        isCounterUp = false;
                        // ENVIA ANTERIOR
                        sendData("prev");
                    }
                }
            }

            if(captureXdata) {
                if(event.values[0] > 2) {
                    captureXdata = false;
                    startXTime = System.currentTimeMillis();
                    if(event.values[0] > 4) {
                        // ENVIAR 20%+

                        sendData(10.toString());
                    } else {
                        // ENVIAR 10%+
                        sendData(5.toString());
                      // ENVIAR 10%+
                    }

                } else if (event.values[0] < -2) {
                    captureXdata = false;
                    startXTime = System.currentTimeMillis();

                    if(event.values[0] < -4) {
                        // ENVIAR 20%-

                        sendData((-10).toString());

                    } else {

                        sendData((-5).toString());
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

                    sendData("next");
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

        return y >= yAxisInitValue
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
