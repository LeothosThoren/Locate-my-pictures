package com.leothos.locatemypictures

import android.Manifest
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val isSDPresent = android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED
    val TAG = this::class.java.simpleName


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Check permissions
        checkPermissions()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //Check if SD card in present in the device.
        // If it's true we can access to the data, else we access the internal storage
        if (isSDPresent) {
            if (android.os.Environment.getExternalStorageState().isNotEmpty())
                retrievePicturesLatLong(
                    getPicturesFromDevice(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                )
        } else {
            retrievePicturesLatLong(
                getPicturesFromDevice(MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            )
        }

    }

    private fun getPicturesFromDevice(uri: Uri): Array<String?> {
        Log.d(TAG, "getPicturesFromDevice Ok")
        val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID)
        val orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC"

        //Stores all the images from the gallery in Cursor
        val cursor = contentResolver.query(
            uri, columns,
            null, null, orderBy
        )

        //Total number of images found
        val count = if (cursor!!.count < LAST_TEN_PICTURES_COUNT) cursor.count else LAST_TEN_PICTURES_COUNT

        //Create an array to store path to the images
        val arrPath = arrayOfNulls<String>(count)

        for (i in 0 until count) {
            cursor.moveToPosition(i)
            val dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            //Store the path of the image
            arrPath[i] = cursor.getString(dataColumnIndex)
            Log.d(TAG, arrPath[i])
        }

        // The cursor should be freed up after use with close()
        cursor.close()

        return arrPath
    }

    private fun retrievePicturesLatLong(arr: Array<String?>) {
        Log.d(TAG, "arr.size = ${arr.size} file name ${arr[0]}")

        //First we check if the array is not null to prevent from crash
        if (arr.isNullOrEmpty()) toast("No pictures found")
        // Second we store all the LatLong info in object in order to retrieve them easily

        else {
            for (i in 0 until arr.size) {
                val exif = ExifInterface(arr[i])
                val latLong = FloatArray(LAT_LONG_ARRAY_SIZE)
                val hasLatLng = exif.getLatLong(latLong)
                if (hasLatLng) {
                    Log.d(TAG, "latitude = ${latLong[0]}, longitude = ${latLong[1]}")
                    addCustomMarkersOnMap(latLong, arr[i]!!, i)
                }
            }
        }

    }

    private fun addCustomMarkersOnMap(latLong: FloatArray, str: String, i :Int) {
        val location = LatLng(latLong[0].toDouble() + i, latLong[1].toDouble() + i)
        val bitmapDrawable = BitmapDrawable.createFromPath(str)
        val d = bitmapDrawable?.current as BitmapDrawable
        val bitmap = d.bitmap
        val smallMarker = Bitmap.createScaledBitmap(bitmap, 200, 200, false)
        mMap.apply {
            addMarker(MarkerOptions().position(location)
                .title("Marker in Sydney")
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)))
            moveCamera(CameraUpdateFactory.newLatLng(location))}
    }

    //**************
    // Permissions
    //**************

    private fun checkPermissions() {
        val permissionListener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                Toast.makeText(this@MapsActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                Toast.makeText(this@MapsActivity, "Permission Denied\n$deniedPermissions", Toast.LENGTH_SHORT).show()
            }

        }

        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
            .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)
            .check()
    }
// Todo check camera.Parameters to set the value of coordinates setGps...cameraCharacteristics

}
