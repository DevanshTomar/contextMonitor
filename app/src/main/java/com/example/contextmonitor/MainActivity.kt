package com.example.contextmonitor

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import com.example.contextmonitor.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private lateinit var cameraControl: CameraControl

    private val accelerometerReadings = AccelerometerData()
    private val vitalSigns = VitalSigns()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        initializeSensors()
        setupUIListeners()
        initializeCamera()
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Toast.makeText(this, "No accelerometer found on this device", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUIListeners() {
        viewBinding.apply {
            buttonRespiratoryRate.setOnClickListener { startRespiratoryRateMeasurement() }
            buttonHeartRate.setOnClickListener { startHeartRateMeasurement() }
            buttonSymptoms.setOnClickListener { navigateToSymptomsActivity() }
        }
    }

    private fun initializeCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    private fun startRespiratoryRateMeasurement() {
        updateUIForMeasurement(isStarting = true, isMeasuringRespiratoryRate = true)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        startMeasurementCountdown(
            onTick = { viewBinding.buttonRespiratoryRate.text = it.toString() },
            onFinish = {
                stopRespiratoryRateMeasurement()
                updateUIForMeasurement(isStarting = false, isMeasuringRespiratoryRate = true)
            }
        )
    }

    private fun stopRespiratoryRateMeasurement() {
        sensorManager.unregisterListener(this)
        val averageRespiratoryRate = calculateAverageRespiratoryRate()
        vitalSigns.respiratoryRate = averageRespiratoryRate
        viewBinding.textViewRespiratory.text = "Respiratory Rate: $averageRespiratoryRate"
        accelerometerReadings.clear()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startHeartRateMeasurement() {
        updateUIForMeasurement(isStarting = true, isMeasuringRespiratoryRate = false)
        startVideoRecording()
    }

    private fun navigateToSymptomsActivity() {
        Toast.makeText(this, "Heart and Respiratory Rate saved!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SymptomsActivity::class.java).apply {
            putExtra("HEART_RATE", vitalSigns.heartRate.toFloat())
            putExtra("RESPIRATORY_RATE", vitalSigns.respiratoryRate.toFloat())
        }
        startActivity(intent)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerReadings.addReading(event.values[0], event.values[1], event.values[2])
            if (accelerometerReadings.isReadyForCalculation()) {
                val respiratoryRate = calculateRespiratoryRate(accelerometerReadings)
                accelerometerReadings.addCalculatedRate(respiratoryRate)
                accelerometerReadings.clear()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun calculateRespiratoryRate(data: AccelerometerData): Int {
        var previousMagnitude = 10f
        var peakCount = 0
        for (i in 11 until data.size) {
            val currentMagnitude = sqrt(
                data.x[i].pow(2) + data.y[i].pow(2) + data.z[i].pow(2)
            )
            if (abs(previousMagnitude - currentMagnitude) > 0.15) {
                peakCount++
            }
            previousMagnitude = currentMagnitude
        }
        return ((peakCount.toDouble() / 45.0) * 30).toInt()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startVideoRecording() {
        val videoCapture = this.videoCapture ?: return
        cameraControl.enableTorch(true)
        viewBinding.buttonHeartRate.isEnabled = false

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        currentRecording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.buttonHeartRate.apply {
                            text = "45"
                            isEnabled = true
                        }
                        startMeasurementCountdown(
                            onTick = { viewBinding.buttonHeartRate.text = it.toString() },
                            onFinish = {
                                currentRecording?.stop()
                                viewBinding.buttonHeartRate.apply {
                                    text = getString(R.string.heart)
                                    isEnabled = true
                                }
                            }
                        )
                    }
                    is VideoRecordEvent.Finalize -> handleRecordingFinalize(recordEvent)
                }
            }
    }

    private fun handleRecordingFinalize(recordEvent: VideoRecordEvent.Finalize) {
        if (!recordEvent.hasError()) {
            val msg = "Video capture succeeded: ${recordEvent.outputResults.outputUri}"
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            Log.d(TAG, msg)

            val uri = recordEvent.outputResults.outputUri
            val path = getFilePathFromUri(uri)
            Log.d(TAG, "Video stored at URI: $uri")
            Log.d(TAG, "Actual file path: $path")

            lifecycleScope.launch {
                val heartRate = calculateHeartRate(uri)
                vitalSigns.heartRate = heartRate
                updateUIAfterHeartRateMeasurement(heartRate)
            }
        } else {
            currentRecording?.close()
            currentRecording = null
            Log.e(TAG, "Video capture ended with error: ${recordEvent.error}")
        }

        cameraControl.enableTorch(false)
        viewBinding.buttonHeartRate.apply {
            text = getString(R.string.heart)
            isEnabled = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun calculateHeartRate(uri: Uri): Int = withContext(Dispatchers.IO) {
        val path = getFilePathFromUri(uri) ?: return@withContext 0
        val retriever = MediaMetadataRetriever()
        val frameList = ArrayList<Bitmap>()

        try {
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toInt() ?: 0
            val frameDuration = min(duration, 425)
            for (i in 10 until frameDuration step 15) {
                retriever.getFrameAtIndex(i)?.let { frameList.add(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting frames: ${e.message}", e)
            return@withContext 0
        } finally {
            retriever.release()
        }

        if (frameList.size < 5) {
            Log.e(TAG, "Not enough frames for heart rate calculation.")
            return@withContext 0
        }

        val redValues = frameList.map { frame ->
            var redSum = 0L
            for (y in 350 until 450) {
                for (x in 350 until 450) {
                    val pixel = frame.getPixel(x, y)
                    redSum += Color.red(pixel) + Color.blue(pixel) + Color.green(pixel)
                }
            }
            redSum
        }

        val smoothedValues = redValues.windowed(5, 1) { it.average().toLong() }
        val peakCount = smoothedValues.zipWithNext().count { (a, b) -> b - a > 3500 }
        ((peakCount.toFloat() * 60) / 4).toInt()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider) }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                cameraControl = camera.cameraControl
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(baseContext, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateUIForMeasurement(isStarting: Boolean, isMeasuringRespiratoryRate: Boolean) {
        viewBinding.apply {
            textViewStatus.text = if (isStarting) "Calculating..." else "Health App"
            buttonSymptoms.visibility = if (isStarting) View.GONE else View.VISIBLE
            if (isMeasuringRespiratoryRate) {
                buttonRespiratoryRate.isEnabled = !isStarting
            } else {
                buttonHeartRate.isEnabled = !isStarting
            }
        }
    }

    private fun updateUIAfterHeartRateMeasurement(heartRate: Int) {
        viewBinding.apply {
            buttonSymptoms.visibility = View.VISIBLE
            textViewHeart.text = "Heart Rate: $heartRate"
            textViewStatus.text = "Health App"
        }
    }

    private fun startMeasurementCountdown(onTick: (Long) -> Unit, onFinish: () -> Unit) {
        object : CountDownTimer(45000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                onTick(millisUntilFinished / 1000)
            }
            override fun onFinish() {
                onFinish()
            }
        }.start()
    }

    private fun calculateAverageRespiratoryRate() = accelerometerReadings.calculatedRates.average().toInt()

    private fun getFilePathFromUri(uri: Uri): String? {
        var filePath: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex("_data")
                filePath = cursor.getString(index)
            }
        }
        return filePath
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}

data class AccelerometerData(
    val x: MutableList<Float> = mutableListOf(),
    val y: MutableList<Float> = mutableListOf(),
    val z: MutableList<Float> = mutableListOf(),
    val calculatedRates: MutableList<Int> = mutableListOf()
) {
    val size: Int
        get() = x.size

    fun addReading(xValue: Float, yValue: Float, zValue: Float) {
        x.add(xValue)
        y.add(yValue)
        z.add(zValue)
    }

    fun addCalculatedRate(rate: Int) {
        calculatedRates.add(rate)
    }

    fun isReadyForCalculation() = size >= 50

    fun clear() {
        x.clear()
        y.clear()
        z.clear()
    }
}

data class VitalSigns(
    var heartRate: Int = 0,
    var respiratoryRate: Int = 0
)