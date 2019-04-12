package com.leothos.locatemypictures.utils

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View.inflate
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.leothos.locatemypictures.CLUSTER_SIZE
import com.leothos.locatemypictures.R
import com.leothos.locatemypictures.model.PictureCluster


data class PictureClusterRenderer(
    val context: AppCompatActivity,
    val googleMap: GoogleMap,
    val clusterManager: ClusterManager<PictureCluster>
) : DefaultClusterRenderer<PictureCluster>(context, googleMap, clusterManager) {

    private val iconGenerator: IconGenerator = IconGenerator(context.applicationContext)
    private val clusterIconGenerator: IconGenerator = IconGenerator(context.applicationContext)
    private val imageView: ImageView
    private val clusterImageView: ImageView
    private val markerWidth: Int = context.resources.getDimension(R.dimen.custom_marker_size).toInt()
    private val markerHeight: Int = context.resources.getDimension(R.dimen.custom_marker_size).toInt()

    init {
        // Set up the views
        val multiPicture = inflate(context, R.layout.multi_picture, null)
        clusterIconGenerator.setContentView(multiPicture)
        clusterImageView = multiPicture.findViewById(R.id.image)

        imageView = ImageView(context.applicationContext)
        imageView.layoutParams = ViewGroup.LayoutParams(markerWidth, markerHeight)
        val padding: Int = context.resources.getDimension(R.dimen.padding_marker).toInt()
        imageView.setPadding(padding, padding, padding, padding)
        iconGenerator.setContentView(imageView)
    }


    override fun onBeforeClusterItemRendered(item: PictureCluster?, markerOptions: MarkerOptions?) {
        // Draw a single picture and set up the info window to show the title / name
        imageView.setImageURI(item?.mPhotoUri)
        val icon = iconGenerator.makeIcon()
        markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(icon))?.title(item?.mTitle)
    }

    override fun onBeforeClusterRendered(cluster: Cluster<PictureCluster>, markerOptions: MarkerOptions) {
        // Draw multiple picture
        val pictures = ArrayList<Drawable>(/*Math.min(4, cluster.size)*/)

        //Draw 4 at most
        for (p in cluster.items) {
            if (pictures.size == CLUSTER_SIZE) break
            val bitmapDrawable = BitmapDrawable.createFromPath((p.mPhotoUri).toString())
            bitmapDrawable?.setBounds(0, 0, markerWidth, markerHeight)
            pictures.add(bitmapDrawable!!)
        }

        Log.d(
            "Debug renderer", "pictures.size = ${pictures.size}\n cluster.items = ${cluster.items}\n" +
                    "cluster.size = ${cluster.size}"
        )

        val multiDrawable = MultiDrawable(pictures)
        multiDrawable.setBounds(0, 0, markerWidth, markerHeight)

        clusterImageView.setImageDrawable(multiDrawable)
        val icon = clusterIconGenerator.makeIcon((cluster.size).toString())
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title
    }

    override fun shouldRenderAsCluster(cluster: Cluster<PictureCluster>): Boolean {
        return cluster.size > 1
    }
}