package com.lalilu.lmusic.ui

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder

class SamplingDraweeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SimpleDraweeView(context, attrs, defStyleAttr) {
    private var samplingValue = 200

    override fun setImageURI(uri: Uri?, callerContext: Any?) {
        super.setImageURI(uri, callerContext)
        if (uri == null) return
        val controllerBuilder = controllerBuilder
        controllerBuilder.setOldController(controller).callerContext = callerContext

        val imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
            .setResizeOptions(ResizeOptions(samplingValue, samplingValue))

        controllerBuilder.imageRequest = imageRequestBuilder.build()
        controllerBuilder.autoPlayAnimations = true
        controller = controllerBuilder.build()
    }
}