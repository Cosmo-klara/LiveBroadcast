package com.example.livebroadcast.handler.opengl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InSurface(
    private val surface: Surface,
    private val textureRenderer: TextureRenderer
) : SurfaceTexture.OnFrameAvailableListener {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var mEGLDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext = EGL14.EGL_NO_CONTEXT
    private var mEGLSurface = EGL14.EGL_NO_SURFACE
    private var surfaceTexture: SurfaceTexture? = null

    var drawSurface: Surface? = null
        private set

    /**
     * 由于[isNewFrameAvailable]是在不同线程中从[SurfaceTexture.OnFrameAvailableListener.onFrameAvailable]和[awaitIsNewFrameAvailable]访问的
     * 用 [Mutex]控制它们不能同时访问。
     */
    private val frameSyncMutex = Mutex()

    // 如果新的视频帧到达则为 true
    private var isNewFrameAvailable = false

    init {
        eglSetup()
    }

    fun createRender(width: Int, height: Int) {
        textureRenderer.surfaceCreated()
        surfaceTexture = SurfaceTexture(textureRenderer.screenRecordTextureId)
        surfaceTexture?.setDefaultBufferSize(width, height)
        surfaceTexture?.setOnFrameAvailableListener(this)
        drawSurface = Surface(surfaceTexture)
    }

    /**
     * 新的视频帧是否将要到来？
     *
     * @return 如果为 true，调用[updateTexImage] [drawImage] [swapBuffers]进行绘制
     */
    suspend fun awaitIsNewFrameAvailable(): Boolean {
        return frameSyncMutex.withLock {
            if (isNewFrameAvailable) {
                isNewFrameAvailable = false
                true
            } else {
                false
            }
        }
    }

    // 从 UI 线程调用
    override fun onFrameAvailable(st: SurfaceTexture) {
        scope.launch {
            frameSyncMutex.withLock {
                // 如果有新的视频帧到来则为 true
                isNewFrameAvailable = true
            }
        }
    }

    fun updateTexImage() {
        textureRenderer.checkGlError("before updateTexImage")
        surfaceTexture?.updateTexImage()
    }

    fun drawImage() {
        val surfaceTexture = surfaceTexture ?: return
        textureRenderer.drawFrame(surfaceTexture)
    }

    fun release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                mEGLDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
        }
        surface.release()
        mEGLDisplay = EGL14.EGL_NO_DISPLAY
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLSurface = EGL14.EGL_NO_SURFACE
        scope.cancel()
    }

    fun makeCurrent() {
        EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
        checkEglError("eglMakeCurrent")
    }

    fun swapBuffers(): Boolean {
        val result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)
        checkEglError("eglSwapBuffers")
        return result
    }

    fun setAltImageTexture(bitmap: Bitmap) {
        textureRenderer.setAltImageTexture(bitmap)
    }

    fun drawAltImage() {
        textureRenderer.drawAltImage()
    }

    private fun eglSetup() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
        checkEglError("eglCreateContext RGB888+recordable ES2")

        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        mEGLContext = EGL14.eglCreateContext(
            mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attrib_list, 0
        )
        checkEglError("eglCreateContext")

        val surfaceAttribs = intArrayOf(
            EGL14.EGL_NONE
        )
        mEGLSurface =
            EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], surface, surfaceAttribs, 0)
        checkEglError("eglCreateWindowSurface")
    }

    private fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            throw RuntimeException("$msg: EGL error: 0x${Integer.toHexString(error)}")
        }
    }

    companion object {
        private const val EGL_RECORDABLE_ANDROID = 0x3142
    }

}