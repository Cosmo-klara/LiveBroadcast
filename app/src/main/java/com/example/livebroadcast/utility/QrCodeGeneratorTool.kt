package com.example.livebroadcast.utility

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

// 二维码生成器
object QrCodeGeneratorTool {

    /**
     * 使用二维码创建库生成二维码
     *
     * @param data 数据
     * @return 二维码位图
     */
    fun generateQrCode(data: String): Bitmap {
        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400)
    }
}