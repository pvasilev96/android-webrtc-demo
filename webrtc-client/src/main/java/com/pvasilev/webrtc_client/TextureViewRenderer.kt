package com.pvasilev.webrtc_client

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView
import org.webrtc.*
import java.util.concurrent.CountDownLatch

class TextureViewRenderer : TextureView, TextureView.SurfaceTextureListener, VideoSink {
    private val eglRenderer: EglRenderer

    private val resourceName: String
        get() = try {
            "${resources.getResourceEntryName(id)}: "
        } catch (e: Resources.NotFoundException) {
            ""
        }

    private val lock = Any()

    private val videoLayoutMeasure = RendererCommon.VideoLayoutMeasure()

    private var isFirstFrameRendered = false

    private var isRenderingPaused = false

    private var rotatedFrameWidth = 0

    private var rotatedFrameHeight = 0

    private var frameRotation = 0

    private var rendererEvents: RendererCommon.RendererEvents? = null

    constructor(context: Context) : super(context) {
        eglRenderer = EglRenderer(resourceName)
        surfaceTextureListener = this
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        eglRenderer = EglRenderer(resourceName)
        surfaceTextureListener = this
    }

    fun init(sharedContext: EglBase.Context, rendererEvents: RendererCommon.RendererEvents?) =
            init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, GlRectDrawer())

    fun init(sharedContext: EglBase.Context, rendererEvents: RendererCommon.RendererEvents?, configAttributes: IntArray, drawer: RendererCommon.GlDrawer) {
        ThreadUtils.checkIsOnMainThread()
        this.rendererEvents = rendererEvents
        synchronized(lock) {
            isFirstFrameRendered = false
            rotatedFrameWidth = 0
            rotatedFrameHeight = 0
            frameRotation = 0
        }
        eglRenderer.init(sharedContext, configAttributes, drawer)
    }

    fun release() = eglRenderer.release()

    fun addFrameListener(listener: EglRenderer.FrameListener, scale: Float, drawerParam: RendererCommon.GlDrawer? = null) =
            if (drawerParam == null) {
                eglRenderer.addFrameListener(listener, scale)
            } else {
                eglRenderer.addFrameListener(listener, scale, drawerParam)
            }

    fun removeFrameListener(listener: EglRenderer.FrameListener) = eglRenderer.removeFrameListener(listener)

    fun setMirror(mirror: Boolean) = eglRenderer.setMirror(mirror)

    fun setScalingType(scalingType: RendererCommon.ScalingType) {
        ThreadUtils.checkIsOnMainThread()
        videoLayoutMeasure.setScalingType(scalingType)
        requestLayout()
    }

    fun setScalingType(scalingTypeMatchOrientation: RendererCommon.ScalingType, scalingTypeMismatchOrientation: RendererCommon.ScalingType) {
        ThreadUtils.checkIsOnMainThread()
        videoLayoutMeasure.setScalingType(scalingTypeMatchOrientation, scalingTypeMismatchOrientation)
        requestLayout()
    }

    fun setFpsReduction(fps: Float) {
        synchronized(lock) {
            isRenderingPaused = fps == 0f
        }
        eglRenderer.setFpsReduction(fps)
    }

    fun disableFpsReduction() {
        synchronized(lock) {
            isRenderingPaused = false
        }
        eglRenderer.disableFpsReduction()
    }

    fun pauseVideo() {
        synchronized(lock) {
            isRenderingPaused = true
        }
        eglRenderer.pauseVideo()
    }

    fun clearImage() = eglRenderer.clearImage()

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        ThreadUtils.checkIsOnMainThread()
        var size: Point? = null
        synchronized(lock) {
            size = videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight)
        }
        if (size != null) {
            setMeasuredDimension(size!!.x, size!!.y)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        ThreadUtils.checkIsOnMainThread()
        eglRenderer.setLayoutAspectRatio((right - left) / (bottom - top).toFloat())
    }

    override fun onFrame(frame: VideoFrame) {
        updateFrameDimensionsAndReportEvents(frame)
        eglRenderer.onFrame(frame)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        ThreadUtils.checkIsOnMainThread()
        eglRenderer.createEglSurface(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        ThreadUtils.checkIsOnMainThread()
        val completionLatch = CountDownLatch(1)
        eglRenderer.releaseEglSurface {
            completionLatch.countDown()
        }
        ThreadUtils.awaitUninterruptibly(completionLatch)
        return true
    }

    private fun updateFrameDimensionsAndReportEvents(frame: VideoFrame) {
        synchronized(lock) {
            if (isRenderingPaused) {
                return
            }
            if (!isFirstFrameRendered) {
                isFirstFrameRendered = true
                if (rendererEvents != null) {
                    rendererEvents?.onFirstFrameRendered()
                }
            }
            if (rotatedFrameWidth != frame.rotatedWidth || rotatedFrameHeight != frame.rotatedHeight
                    || frameRotation != frame.rotation) {
                if (rendererEvents != null) {
                    rendererEvents?.onFrameResolutionChanged(frame.rotatedWidth, frame.rotatedHeight, frame.rotation)
                }
                rotatedFrameWidth = frame.rotatedWidth
                rotatedFrameHeight = frame.rotatedHeight
                frameRotation = frame.rotation
                post {
                    requestLayout()
                }
            }
        }
    }
}