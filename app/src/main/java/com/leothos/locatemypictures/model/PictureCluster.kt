package com.leothos.locatemypictures.model

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class PictureCluster(
    val mTitle: String? = null,
    val mSnippet: String? = null,
    val mPhotoUri: Uri,
    val mLocation: LatLng
) : ClusterItem {

    override fun getSnippet(): String? {
        return mSnippet
    }

    override fun getTitle(): String? {
        return mTitle
    }

    override fun getPosition(): LatLng {
        return mLocation
    }

}