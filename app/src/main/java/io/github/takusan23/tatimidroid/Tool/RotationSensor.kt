package io.github.takusan23.tatimidroid.Tool

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity

class RotationSensor(context: AppCompatActivity) {

    var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    var sensorEventListener: SensorEventListener

    //加速度の値。配列になっている
    var accelerometerList = floatArrayOf()

    init {
        //加速度
        val accelerometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)
        //受け取る
        sensorEventListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                //つかわん
            }

            override fun onSensorChanged(event: SensorEvent?) {
                //値はここで受けとる
                when (event?.sensor?.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        //加速度
                        accelerometerList = event.values.clone()
                        //画面回転
                        if (accelerometerList[0] >= 9) {
                            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else if (accelerometerList[0] <= -9) {
                            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                }
            }
        }
        //加速度センサー登録
        sensorManager.registerListener(
            sensorEventListener,
            accelerometer[0],  //配列のいっこめ。
            SensorManager.SENSOR_DELAY_NORMAL  //更新頻度
        )
    }

    /*
    * onDestroyとかに書いてね
    * */
    fun destroy() {
        sensorManager.unregisterListener(sensorEventListener)
    }

}