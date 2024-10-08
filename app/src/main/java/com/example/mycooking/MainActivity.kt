package com.example.mycooking

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.view.animation.AnimationUtils

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: Button
    private lateinit var cameraIcon: ImageView
    private lateinit var topText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var backgroundImage: ImageView
    private lateinit var zoomImage: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var scanText: TextView
    private lateinit var preScan: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.captureButton)
        topText = findViewById(R.id.topText)
        cameraIcon = findViewById(R.id.cameraIcon)
        backgroundImage = findViewById(R.id.backgroundImage)
        zoomImage = findViewById(R.id.zoomImage)
        progressBar = findViewById(R.id.progressBar)
        scanText = findViewById(R.id.scanText)
        preScan = findViewById(R.id.preScan)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val pulsateAnimation = AnimationUtils.loadAnimation(this, R.anim.pulsate_animation)
        topText.startAnimation(pulsateAnimation)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        captureButton.setOnClickListener {
            if (allPermissionsGranted()) {
                fadeToCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
        handleIntent(intent)
    }




    private fun fadeToCamera() {
        val fadeOutDuration: Long = 1000

        // Fade out the background image and capture button
        backgroundImage.animate()
            .alpha(0f)
            .setDuration(fadeOutDuration)
            .start()

        captureButton.animate()
            .alpha(0f)
            .setDuration(fadeOutDuration)
            .start()

        topText.animate()
            .alpha(0f)
            .setDuration(fadeOutDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    backgroundImage.visibility = View.GONE
                    captureButton.visibility = View.GONE
                    topText.visibility = View.GONE

                    // Start the camera
                    startCamera()

                    // Fade in the camera
                    viewFinder.alpha = 0f
                    viewFinder.visibility = View.VISIBLE
                    viewFinder.animate()
                        .alpha(1f)
                        .setDuration(fadeOutDuration)
                        .start()

                    cameraIcon.alpha = 0f
                    cameraIcon.visibility = View.VISIBLE
                    cameraIcon.animate()
                        .alpha(1f)
                        .setDuration(fadeOutDuration)
                        .start()
                }
            })
        }

    private fun startCamera() {
        // Make sure permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSIONS
            )
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Build the preview use case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            preScan.visibility = View.VISIBLE

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            // Only use the back camera to scan the fridge
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                cameraIcon.setOnClickListener {
                    val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                    cameraIcon,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.3f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.3f)
                )
                    // Indicates camera was clicked on
                    scaleUp.duration = 500
                    scaleUp.repeatCount = 1
                    scaleUp.repeatMode = ValueAnimator.REVERSE
                    scaleUp.start()

                    takePhoto() }
            } catch (exc: Exception) {
                Toast.makeText(this, "Use case binding failed: ${exc.message}", Toast.LENGTH_SHORT)
                    .show()
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun takePhoto() {
        preScan.visibility = View.GONE
        progressBar.translationZ = 4f
        scanText.translationZ = 4f
        scanText.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        val imageCapture = imageCapture ?: return

        progressBar.progress = 0 // Reset progress bar

        // Apply pulsating animation
        val pulsateAnimation = AnimationUtils.loadAnimation(this, R.anim.pulsate_animation)
        scanText.startAnimation(pulsateAnimation)

        val timer = Timer()
        var timerTask: TimerTask

        fun startTimer() {
            timerTask = object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        // Update the progress value
                        progressBar.progress += 10

                        // Check if the progress is complete
                        if (progressBar.progress >= progressBar.max) {
                            timer.cancel()
                        }
                    }
                }
            }
            timer.schedule(timerTask, 0, 1050)
        }

        startTimer()


        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    // Convert the ImageProxy to a ByteArray
                    val byteArray = imageProxyToByteArray(imageProxy)

                    // Upload the image to the Flask server
                    uploadImage(byteArray)

                    // Close the image proxy
                    imageProxy.close()
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        baseContext,
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    timer.cancel()
                    progressBar.visibility = View.GONE
                    scanText.visibility = View.GONE
                    scanText.clearAnimation()
                    preScan.visibility = View.VISIBLE
                }
            }
        )
    }



    private fun imageProxyToByteArray(imageProxy: ImageProxy): ByteArray {
        val buffer = imageProxy.planes[0].buffer
        val byteArray = ByteArray(buffer.remaining())
        buffer.get(byteArray)
        return byteArray
    }


    private fun uploadImage(imageByteArray: ByteArray) {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "image.jpg",
                imageByteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            // * REPLACE WITH "http://YOUR_IP:PORT/upload" TO RUN LOCALLY, 
            // FLASK SHOWS IT IN TERMINAL AFTER RUNNING *
            .url("http://YOUR_IP:5000/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(baseContext, "Upload failed:${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    progressBar.visibility = View.GONE
                    scanText.visibility = View.GONE
                    scanText.clearAnimation()
                    preScan.visibility = View.VISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("MainActivity", "Response body: $responseBody")
                runOnUiThread {
                    try {
                        //Reset cam UI
                        progressBar.visibility = View.GONE
                        scanText.visibility = View.GONE
                        scanText.clearAnimation()
                        preScan.visibility = View.VISIBLE

                        if (response.isSuccessful && responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            val dishesText = jsonResponse.optString("dishes", "No dishes found")


                            // Start RecipesActivity with the dishes data
                            val intent = Intent(this@MainActivity, RecipesActivity::class.java)
                            intent.putExtra("dishes", dishesText)
                            startActivity(intent)
                        } else {
                            // Handle case where response is not successful
                            val errorMessage = try {
                                JSONObject(responseBody ?: "").optString("error", "Unknown error")
                            } catch (e: Exception) {
                                "Upload failed: ${response.message}"
                            }
                            Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error processing response", e)
                        Toast.makeText(
                            baseContext,
                            "Error processing response: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
    // Retakes photos if needed
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "retakePhoto") {
            startCamera()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
