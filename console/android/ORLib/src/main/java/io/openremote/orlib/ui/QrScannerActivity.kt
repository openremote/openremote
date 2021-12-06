package io.openremote.orlib.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import io.openremote.orlib.R
import io.openremote.orlib.databinding.ActivityOrQrScannerBinding
import io.openremote.orlib.service.QrScannerProvider
import java.io.IOException

class QrScannerActivity : AppCompatActivity() {
    private var binding: ActivityOrQrScannerBinding? = null
    private var surfaceView: SurfaceView? = null
    private var barcodeDetector: BarcodeDetector? = null
    private var cameraSource: CameraSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrQrScannerBinding.inflate(layoutInflater)
        val view = binding!!.root

        setContentView(view)
        supportActionBar!!.title = "Scan QR Code"
        initViews()
        initialiseDetectorsAndSources()
    }

    private fun initViews() {
        surfaceView = findViewById(R.id.surfaceView)
    }

    private fun initialiseDetectorsAndSources() {
        barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setAutoFocusEnabled(true) //you should add this feature
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .build()
        surfaceView!!.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@QrScannerActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        cameraSource!!.start(surfaceView!!.holder)
                    } else {
                        ActivityCompat.requestPermissions(
                            this@QrScannerActivity,
                            arrayOf(Manifest.permission.CAMERA),
                            REQUEST_CAMERA_PERMISSION
                        )
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource!!.stop()
            }
        })
        barcodeDetector!!.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
            }

            override fun receiveDetections(detections: Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if (barcodes.size() != 0) {
                    val data = Intent()
                    data.putExtra("result", barcodes.valueAt(0).displayValue)
                    setResult(RESULT_OK, data)
                    finish()
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraSource!!.start(surfaceView!!.holder)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.camera_needed_alert_title)
                .setMessage(R.string.camera_needed_alert_body)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes) { dialog, which ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                }
                .show()
        }
    }

    override fun onDestroy() {
        if (barcodeDetector != null) {
            barcodeDetector!!.release()
            barcodeDetector = null
        }

        if (cameraSource != null) {
            cameraSource!!.stop()
            cameraSource!!.release()
            cameraSource = null
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 201
    }
}