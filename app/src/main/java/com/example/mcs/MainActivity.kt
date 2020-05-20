package com.example.mcs

import android.R.attr.x
import android.R.attr.y
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), SensorEventListener {

    var sensorManager: SensorManager? = null;
    var accSensor: Sensor? = null;
    var captureXdata = true;
    var startXTime: Long? = null;
    private val maxVertical = 3.0
    private var mStartTime: Long = 0
    private var isCounterUp = false;
    private val SHAKE_THRESHOLD = 800
    private var last_x: Float = 0f;
    private var last_y: Float = 0f;
    private var last_z: Float = 0f;
    private var lastUpdate: Long = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // TODO
    }

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

                var x = 0f;
                var y = 0f;
                var z = 0f;

                val diffTime: Long = curTime - lastUpdate
                lastUpdate = curTime
                x = event.values[0]
                y = event.values[1]
                z = event.values[2]

                val speed: Float =
                    Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000
                if (speed > SHAKE_THRESHOLD) {
                    // PRÓXIMA
                    Log.d("XYZ", "PROXIMA")
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

}
