package com.deltainteraction.orion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log

class ScreenCaptureActivity : Activity() {

    private var TAG = "ScreenCaptureActivity"
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val REQUEST_MEDIA_PROJECTION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MediaProjectionManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Start the screen capture intent
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val resultIntent = Intent("com.deltainteraction.ACTION_SCREEN_CAPTURE")
            .setPackage(packageName)
        resultIntent.putExtra("resultCode", resultCode)
        data?.let { resultIntent.putExtra("data", it) }
        sendBroadcast(resultIntent)

        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "Broadcast sent with screen capture result.")
        } else {
            Log.e(TAG, "Screen capture permission was not granted.")
        }
        finish() // Close the activity once the result is processed
    }
}
