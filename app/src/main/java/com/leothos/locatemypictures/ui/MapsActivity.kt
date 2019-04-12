package com.leothos.locatemypictures.ui

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.leothos.locatemypictures.*
import com.leothos.locatemypictures.model.PictureCluster
import com.leothos.locatemypictures.utils.PictureClusterRenderer


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    //val
    private val isSDPresent = android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED
    private val TAG = this::class.java.simpleName
    private val externalStorage = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val internalStorage = MediaStore.Images.Media.INTERNAL_CONTENT_URI
    private val defaultLatLong = LatLng(DEFAULT_LAT, DEFAULT_LNG)


    //var
    private lateinit var clusterManager: ClusterManager<PictureCluster>
    private lateinit var pictureRenderer: PictureClusterRenderer
    private lateinit var mMap: GoogleMap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Check permissions
        checkPermissions()
        // Check camera parameters
        checkGpsCameraParameters()
    }

    override fun onResume() {
        super.onResume()
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

        // First setting up the cluster
        setUpClusters()

        //Check if SD card is present in the device.
        // If it's true we can access to the external storage, else we access internal storage
        if (isSDPresent) {
            if (android.os.Environment.getExternalStorageState().isNotEmpty())
                retrievePicturesExifLatLong(getPicturesPathFromUri(externalStorage))

        } else {
            retrievePicturesExifLatLong(getPicturesPathFromUri(internalStorage))
        }

    }

    /**
     *
     * */
    private fun getPicturesPathFromUri(uri: Uri): Array<String?> {
        Log.d(TAG, "getPicturesPathFromUri Ok")

        val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID)
        val orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC"

        //Stores all the images from the gallery in Cursor
        val cursor = contentResolver.query(
            uri, columns, null, null, orderBy
        )

        //Total number of images found
        val count = if (cursor!!.count < LAST_TEN_PICTURES_COUNT) cursor.count else LAST_TEN_PICTURES_COUNT

        //Create an array to store path to the images
        val filesPath = arrayOfNulls<String>(count)
        for (i in 0 until count) {
            cursor.moveToPosition(i)
            val dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            //Store the path of the image
            filesPath[i] = cursor.getString(dataColumnIndex)
            Log.d(TAG, filesPath[i])
        }

        // The cursor should be freed up after use with close()
        cursor.close()
        return filesPath
    }

    private fun retrievePicturesExifLatLong(filePathArray: Array<String?>) {

        //First we check if the array is not null to prevent from crash
        if (filePathArray.isNullOrEmpty()) toast("No pictures found")

        // Second we store all the LatLong info and more into a list in order to retrieve them easily
        else {
            for (i in 0 until filePathArray.size) {
                Log.d(TAG, "content of the array : ${filePathArray[i]}")

                val exif = ExifInterface(filePathArray[i]!!)
                val latLong = FloatArray(LAT_LONG_ARRAY_SIZE)
                val hasLatLng = exif.getLatLong(latLong)

                if (hasLatLng) {
                    Log.d(TAG, "latitude = ${latLong[0]}, longitude = ${latLong[1]}")
                    addCustomRendererMarkersOnMap(position(latLong, i), filePathArray[i])
                }
            }
        }

    }


    // **************
    // Clustering
    // **************

    private fun setUpClusters() {

        clusterManager = ClusterManager(this.applicationContext, mMap)
        pictureRenderer = PictureClusterRenderer(this, mMap, clusterManager)
        clusterManager.renderer = pictureRenderer
        mMap.setOnCameraIdleListener(clusterManager)
    }

    // Renderer methods
    private fun addCustomRendererMarkersOnMap(latLong: LatLng, str: String?) {

        //set camera default position
        setCameraMapPosition()
        //Create picture cluster object
        val pictureCluster = PictureCluster(mPhotoUri = Uri.parse(str), mLocation = latLong)
        // Add to cluster manager
        clusterManager.addItem(pictureCluster)
        clusterManager.cluster()

    }

    private fun setCameraMapPosition() {
        // Show the icon on the map
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLong, CONTINENT_ZOOM))

    }

    // Simple utils method to generate latLong
    private fun position(latLong: FloatArray, i: Int): LatLng {
        val trick = i - 1
        return LatLng(latLong[0].toDouble() + trick, latLong[1].toDouble() + trick)
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
            .setDeniedMessage(
                "If you reject permission,you can not use this service" +
                        "\n\nPlease turn on permissions at [Setting] > [Permission]"
            )
            .setPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
//                Manifest.permission.CAMERA
            )
            .check()
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun checkGpsCameraParameters() {
        val cameraManager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraList = cameraManager.cameraIdList
        if (cameraList.isNotEmpty()) {
            for (c in cameraList)
                Log.d(TAG, "list camera id => $c or ${cameraList[0]}")
        }

    }

}
