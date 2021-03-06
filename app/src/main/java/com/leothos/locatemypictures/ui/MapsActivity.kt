package com.leothos.locatemypictures.ui

import android.Manifest
import android.media.ExifInterface
import android.net.Uri
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
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.leothos.locatemypictures.*
import com.leothos.locatemypictures.model.PictureCluster
import com.leothos.locatemypictures.utils.PictureClusterRenderer

// Todo : update the image zoom
// Todo : add a click listener both on Cluster or marker
// Todo : Reduce the lags


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, ClusterManager.OnClusterClickListener<PictureCluster> {


    //val
    private val isSDPresent =
        android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED
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

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Check permissions
        checkPermissions()

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
                addCustomRendererMarkersOnMap(retrievePicturesExifLatLong(getPicturesPathFromUri(externalStorage)))

        } else {
            addCustomRendererMarkersOnMap((retrievePicturesExifLatLong(getPicturesPathFromUri(internalStorage))))
        }

        clusterManager.setOnClusterItemClickListener {
            toast("test click listener")
            true }
    }


    //==========
    // Actions
    //==========

    override fun onClusterClick(cluster: Cluster<PictureCluster>?): Boolean {
        toast("${cluster?.size} (including ${cluster?.position})")
        return false
    }

    /**
     * Retrieve 10 or less pictures in Media repository of the device and store them into a Cursor object
     * All the path are displayed into an array of string. The file path will help to retrieve Exif meta data
     *
     * */
    private fun getPicturesPathFromUri(uri: Uri): Array<String?> {
        Log.d(TAG, "getPicturesPathFromUri Ok")

        val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID)
        val orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC"

        //Stores all the images from the gallery in Cursor
        val cursor = contentResolver.query(
            uri, columns, MediaStore.Images.Media.DATA + " like ? ", arrayOf("%Camera%"), orderBy
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

    /**
     * This method take a file path in parameter and allow to retrieve exif meta data.
     * In particular Latlong attribute
     * */
    private fun retrievePicturesExifLatLong(filePathArray: Array<String?>): MutableList<PictureCluster> {
        val pictureCluster = mutableListOf<PictureCluster>()
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

                    //Create picture cluster object
                    pictureCluster.add(
                        PictureCluster(
                            mPhotoUri = Uri.parse(filePathArray[i]),
                            mLocation = position(latLong)
                        )
                    )
                }
            }

        }
        return pictureCluster
    }

    // **************
    // Clustering
    // **************

    /**
     *
     * */
    private fun setUpClusters() {

        clusterManager = ClusterManager(this.applicationContext, mMap)
        pictureRenderer = PictureClusterRenderer(this, mMap, clusterManager)
        clusterManager.renderer = pictureRenderer
        mMap.setOnCameraIdleListener(clusterManager)
        //click handler
        mMap.setOnMarkerClickListener(clusterManager)
        clusterManager.setOnClusterClickListener(this)
    }

    // Renderer methods
    private fun addCustomRendererMarkersOnMap(pictureCluster: MutableList<PictureCluster>) {

        // Add to cluster manager
        for (p in pictureCluster)
            clusterManager.addItem(p)

        // After adding item
        clusterManager.cluster()

        setCameraMapPosition(pictureCluster[0].mLocation)

    }

    private fun setCameraMapPosition(latLng: LatLng) {
        // Show the icon on the map
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CONTINENT_ZOOM))
    }

    // Simple utils method to generate latLong
    private fun position(latLong: FloatArray): LatLng {
        return LatLng(latLong[0].toDouble(), latLong[1].toDouble())
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
            )
            .check()
    }

}
