package com.jamgu.common.util.bitmap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.jamgu.common.util.log.JLog.e
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object BitmapUtils {
    private const val TAG = "BitmapUtils"
    private const val DEFAULT_QUALITY = 90

    //最大bitmap宽
    const val MAX_BITMAP_WIDTH = 1440

    //最大bitmap高
    const val MAX_BITMAP_HEIGHT = 2560
    fun compressToBytes(
        bitmap: Bitmap,
        format: CompressFormat?
    ): ByteArray {
        return compressToBytes(bitmap, DEFAULT_QUALITY, format)
    }

    @JvmOverloads
    fun compressToBytes(
        bitmap: Bitmap, quality: Int = DEFAULT_QUALITY,
        format: CompressFormat? =
            CompressFormat.JPEG
    ): ByteArray {
        val baos = ByteArrayOutputStream(65536)
        bitmap.compress(format, quality, baos)
        return baos.toByteArray()
    }

    fun getImageRotationAngleInDegree(imagePath: String): Int? {
        if (TextUtils.isEmpty(imagePath)) return null
        try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            var result = 0
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    result = 90
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    result = 180
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    result = 270
                }
            }
            return result
        } catch (e: Exception) {
            e(TAG, "get image rotation angle failed :" + imagePath + "," + e.message, e)
        }
        return null
    }

    @SuppressLint("InlinedApi")
    fun processExif(bitmap: Bitmap?, imagePath: String): Bitmap? {
        val rotation = getImageRotationAngleInDegree(imagePath) ?: return bitmap
        return rotateBitmap(bitmap, rotation)
    }

    fun rotateBitmap(bitmap: Bitmap?, rotation: Int): Bitmap? {
        var rotation = rotation
        if (bitmap == null) return null
        rotation = rotation % 360
        if (rotation == 0) {
            // do nothing.
            return bitmap
        }
        val rotateDimension = (rotation > 45 && rotation < 135
                || rotation > 225 && rotation < 315)
        val width = if (!rotateDimension) bitmap.width else bitmap.height
        val height = if (!rotateDimension) bitmap.height else bitmap.width
        var newBitmap: Bitmap? = null
        try {
            newBitmap = Bitmap.createBitmap(width, height, bitmap.config)
        } catch (e: Throwable) {
            // do nothing.
        }
        if (newBitmap == null || newBitmap == bitmap) {
            // no enough memory or original bitmap returns.
            return bitmap
        }
        val canvas = Canvas(newBitmap)
        val dx = (width - bitmap.width) / 2
        val dy = (height - bitmap
                .height) / 2
        if (dx != 0 || dy != 0) {
            canvas.translate(dx.toFloat(), dy.toFloat())
        }
        canvas.rotate(rotation.toFloat(), (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        // recycle prev bitmap.
        bitmap.recycle()
        return newBitmap
    }

    fun drawViewToBitmap(
        dest: Bitmap?, view: View, width: Int,
        height: Int, downSampling: Int, drawable: Drawable
    ): Bitmap? {
        var dest = dest
        Log.d("drawViewToBitmap", "view$width $height")
        val scale = 1f / downSampling
        val heightCopy = view.height
        view.layout(0, 0, width, height)
        val bmpWidth = (width * scale).toInt()
        val bmpHeight = (height * scale).toInt()
        Log.d("drawViewToBitmap", "bmpview$bmpWidth $bmpHeight")
        Log.d("drawViewToBitmap", "heightCopy$heightCopy")
        if (dest == null || dest.width != bmpWidth || dest.height != bmpHeight) {
            dest = Bitmap.createBitmap(
                bmpWidth, bmpHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val c = Canvas(dest!!)
        drawable.bounds = Rect(0, 0, width, height)
        drawable.draw(c)
        if (downSampling > 1) {
            c.scale(scale, scale)
        }
        view.draw(c)
        view.layout(0, 0, width, heightCopy)
        // saveToSdCard(dest, "dest"+System.currentTimeMillis()+".png");
        return dest
    }

    fun getBitmapFromView(v: View): Bitmap? {
        val willNotCache = v.willNotCacheDrawing() // 返回视图是否可以保存他的画图缓存
        v.setWillNotCacheDrawing(false)

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation //将视图在此操作时置为透明
        val color = v.drawingCacheBackgroundColor // 获得绘制缓存位图的背景颜色
        v.drawingCacheBackgroundColor = 0 // 设置绘图背景颜色
        if (color != 0) { // 如果获得的背景不是黑色的则释放以前的绘图缓存
            v.destroyDrawingCache() // 释放绘图资源所使用的缓存
        }
        v.buildDrawingCache() // 重新创建绘图缓存，此时的背景色是黑色
        val cacheBitmap = v.drawingCache ?: return null // 将绘图缓存得到的,注意这里得到的只是一个图像的引用
        val bitmap = Bitmap.createBitmap(cacheBitmap) // 将位图实例化
        // Restore the view //恢复视图
        v.destroyDrawingCache() // 释放位图内存
        v.setWillNotCacheDrawing(willNotCache) // 返回以前缓存设置
        v.drawingCacheBackgroundColor = color // 返回以前的缓存颜色设置
        return bitmap
    }

    fun getBitmapSize(bitmap: Bitmap): Int {
        return if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
            bitmap.byteCount
        } else {
            bitmap.rowBytes * bitmap.height
        }
    }

    fun compressByQuality(bitmap: Bitmap?, maxSize: Long): ByteArray? {
        if (bitmap == null || maxSize <= 0) {
            return null
        }
        ByteArrayOutputStream().use {
            var result: ByteArray? = null
            var quality = 70
            while (true) {
                try {
                    it.reset()
                    bitmap.compress(CompressFormat.JPEG, quality, it)
                    result = it.toByteArray()
                } catch (e: Throwable) {
                    quality -= 10
                    if (quality <= 0) {
                        break
                    }
                    continue
                }
                quality -= if (quality >= 30) {
                    10
                } else {
                    5
                }
                if (result != null && result.size <= maxSize) {
                    break
                }
            }
            return result
        }
    }

    fun getBitmapFromView(v: View, width: Int, height: Int): Bitmap? {
        val willNotCache = v.willNotCacheDrawing() // 返回视图是否可以保存他的画图缓存
        v.setWillNotCacheDrawing(false)

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation //将视图在此操作时置为透明
        val color = v.drawingCacheBackgroundColor // 获得绘制缓存位图的背景颜色
        v.drawingCacheBackgroundColor = 0 // 设置绘图背景颜色
        if (color != 0) { // 如果获得的背景不是黑色的则释放以前的绘图缓存
            v.destroyDrawingCache() // 释放绘图资源所使用的缓存
        }
        v.buildDrawingCache() // 重新创建绘图缓存，此时的背景色是黑色
        val cacheBitmap = v.drawingCache ?: return null // 将绘图缓存得到的,注意这里得到的只是一个图像的引用
        // Bitmap bitmap = Bitmap.createBitmap(cacheBitmap); // 将位图实例化
        val bitmap = Bitmap.createBitmap(cacheBitmap, 0, 0, width, height)
        // Restore the view //恢复视图
        v.destroyDrawingCache() // 释放位图内存
        v.setWillNotCacheDrawing(willNotCache) // 返回以前缓存设置
        v.drawingCacheBackgroundColor = color // 返回以前的缓存颜色设置
        return bitmap
    }

    fun saveBitmapDatasToFile(datas: ByteArray?, filePath: String?) {
        if (datas == null) {
            return
        }
        FileOutputStream(filePath).use {
            try {
                it.write(datas)
            } catch (e: Exception) {
                e(TAG, e.message, e)
            }
        }
    }

    @JvmOverloads
    fun compressBySize(
        filePath: String?,
        maxWidth: Int,
        maxHeight: Int,
        config: Bitmap.Config? = Bitmap.Config.ARGB_8888
    ): Bitmap? {
        var opts: BitmapFactory.Options? = null
        if (config != null) {
            opts = BitmapFactory.Options()
            opts.inPreferredConfig = config
        }
        return compressBySize(filePath, maxWidth, maxHeight, opts)
    }

    fun compressBySize(filePath: String?, maxWidth: Int, maxHeight: Int, opts: BitmapFactory.Options?): Bitmap? {
        var lOpts = opts
        if (TextUtils.isEmpty(filePath) || maxWidth <= 0 || maxHeight <= 0) {
            return null
        }
        if (lOpts == null) {
            lOpts = BitmapFactory.Options()
        }
        lOpts.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, lOpts)
        // 得到图片的宽度、高度；
        val imgWidth = lOpts.outWidth
        val imgHeight = lOpts.outHeight
        // 分别计算图片宽度、高度与目标宽度、高度的比例；取大于该比例的最小整数；
        val widthRatio = Math.ceil((imgWidth / maxWidth.toFloat()).toDouble()).toInt()
        val heightRatio = Math.ceil((imgHeight / maxHeight.toFloat()).toDouble()).toInt()
        if (widthRatio > 1 && heightRatio > 1) {
            if (widthRatio > heightRatio) {
                lOpts.inSampleSize = widthRatio
            } else {
                lOpts.inSampleSize = heightRatio
            }
        }
        // 设置好缩放比例后，加载图片进内存；
        lOpts.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, lOpts)
    }

    fun compressBySize(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        var bitmap = bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 100, baos)
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().size, opts)
        val imgWidth = opts.outWidth
        val imgHeight = opts.outHeight
        val widthRatio = Math.ceil((imgWidth.toFloat() / targetWidth.toFloat()).toDouble()).toInt()
        val heightRatio = Math.ceil((imgHeight.toFloat() / targetHeight.toFloat()).toDouble()).toInt()
        if (widthRatio > 1 && heightRatio > 1) {
            if (widthRatio > heightRatio) {
                opts.inSampleSize = widthRatio
            } else {
                opts.inSampleSize = heightRatio
            }
        }
        opts.inJustDecodeBounds = false
        bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().size, opts)
        return bitmap
    }

    /**
     * 保存图片到SD卡目录下
     *
     * @return 返回路径
     */
    fun saveBitmap(bitmap: Bitmap?, path: String?): String? {
        if (path == null || bitmap == null) return null
        var bos: BufferedOutputStream? = null
        return try {
            bos = BufferedOutputStream(FileOutputStream(path))
            bitmap.compress(CompressFormat.JPEG, 100, bos)
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                bos?.flush()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            closeBufferedOutputStream(bos)
        }
    }

    private fun closeBufferedOutputStream(bos: BufferedOutputStream?) {
        if (bos != null) {
            try {
                bos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * @param context   上下文
     * @param VideoPath 视频的路径
     */
    fun getVideoThumbnail(
        context: Context,
        VideoPath: String
    ): Bitmap? {
        val testcr = context.contentResolver
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media._ID
        )
        val whereClause = (MediaStore.Video.Media.DATA + " = '" + VideoPath
                + "'")
        val cursor = testcr.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
            whereClause, null, null
        )
        var _id = 0
        var videoPath: String? = ""
        if (cursor == null) {
            return null
        }
        if (cursor.count == 0) {
            cursor.close()
            return null
        }
        if (cursor.moveToFirst()) {
            val _idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID)
            val _dataColumn = cursor
                    .getColumnIndex(MediaStore.Video.Media.DATA)
            do {
                _id = cursor.getInt(_idColumn)
                videoPath = cursor.getString(_dataColumn)
            } while (cursor.moveToNext())
        }
        cursor.close()
        val options = BitmapFactory.Options()
        options.inDither = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return MediaStore.Video.Thumbnails.getThumbnail(
            testcr, _id.toLong(),
            MediaStore.Images.Thumbnails.MINI_KIND, options
        )
    }

    fun getPicWidthHeight(filePath: String?): IntArray {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        return intArrayOf(options.outWidth, options.outHeight)
    }

    private fun decodeBounds(path: String): BitmapFactory.Options? {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        try {
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            options.inJustDecodeBounds = false
        } catch (tr: Throwable) {
            tr.printStackTrace()
            return null
        }
        return options
    }

    fun calculateInSampleSize(path: String, reqWidth: Int, reqHeight: Int): Int {
        val options = decodeBounds(path)
        // Raw height and width of image
        val height = options!!.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /***
     * 通过View视图获取View视图的图片
     *
     * @return 保存的图片文件的路径
     */
    fun getImageFileFromView(view: View?, destFilePath: String?): String? {
        if (TextUtils.isEmpty(destFilePath)) {
            return null
        }
        val bitmap = loadBitmapFromView(view) ?: return null
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(File(destFilePath))
            bitmap.compress(CompressFormat.JPEG, 90, fos)
            fos.flush()
        } catch (e: IOException) {
            e(TAG, e.message, e)
            return null
        } finally {
            try {
                fos!!.close()
            } catch (e: Exception) {
                e(TAG, e.message, e)
            }
            bitmap.recycle()
        }
        return destFilePath
    }

    private fun loadBitmapFromView(v: View?): Bitmap? {
        if (v == null) {
            return null
        }
        val screenshot: Bitmap
        return if (v.width > 0 && v.height > 0) {
            screenshot = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(screenshot)
            c.translate(-v.scrollX.toFloat(), -v.scrollY.toFloat())
            v.draw(c)
            screenshot
        } else {
            null
        }
    }

    /**
     * 生成一张输入View的截图
     *
     * @param view
     * @return
     */
    fun snapshotView(view: View?): Bitmap? {
        return loadBitmapFromView(view)
    }

    fun getBitmap(path: String?): Bitmap? {
        return if (TextUtils.isEmpty(path)) null else BitmapFactory.decodeFile(path)
    }

    /**
     * 根据要求的大小生成Bitmap
     *
     * @param path           文件路径
     * @param requiredWidth  请求宽度
     * @param requiredHeight 请求高度
     * @return
     */
    fun getBitmapBySize(
        path: String?, requiredWidth: Int,
        requiredHeight: Int
    ): Bitmap? {
        if (TextUtils.isEmpty(path) || requiredHeight <= 0 || requiredWidth <= 0) return null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val inSampleSize = Math.min(
            options.outWidth / requiredWidth,
            options.outHeight / requiredHeight
        )
        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        // 当inPurgeable==true时，可以让java系统内存不足时先行回收部分的内存
        options.inPurgeable = true
        return BitmapFactory.decodeFile(path, options)
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val ratioX = newWidth / bitmap.width.toFloat()
        val ratioY = newHeight / bitmap.height.toFloat()
        val middleX = newWidth / 2.0f
        val middleY = newHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = Canvas(scaledBitmap)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(
            bitmap,
            middleX - bitmap.width / 2,
            middleY - bitmap.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return scaledBitmap
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 将图片地址解析成Bitmap，然后缩放。最大边长不超过maxLineLength。
     *
     * @param path          图片地址
     * @param maxLineLength 缩放的最大边长
     * @return 缩放后的Bitmap
     */
    fun resizeBitmap(path: String?, maxLineLength: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val outWidth = options.outWidth
        val outHeight = options.outHeight
        var reSize = outWidth / maxLineLength
        val mod = outWidth % maxLineLength
        if (mod > 0) {
            reSize += 1
        }
        if (outWidth < outHeight) {
            reSize = outHeight / maxLineLength
            val mode = outHeight % maxLineLength
            if (mode > 0) {
                reSize += 1
            }
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = reSize
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeFile(path, options)
    }

    /**
     * 获取资源图片宽高
     *
     * @param context
     * @param resId
     * @return size[0] 宽度 size[1] 高度
     */
    fun getSize(context: Context?, resId: Int): IntArray {
        val size = IntArray(2)
        if (context != null && resId != 0) {
            val option = BitmapFactory.Options()
            option.inJustDecodeBounds = true
            BitmapFactory.decodeResource(context.resources, resId, option)
            size[0] = option.outWidth
            size[1] = option.outHeight
        }
        return size
    }

    /**
     * 获取资源图片的宽高
     *
     * @param path
     * @return
     */
    fun getSize(path: String?): IntArray {
        val size = IntArray(2)
        if (path != null && !TextUtils.isEmpty(path)) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            size[0] = options.outWidth
            size[1] = options.outHeight
        }
        return size
    }

    fun bitmapToBytes(bmp: Bitmap?): ByteArray? {
        if (bmp == null) return null
        val bos = ByteArrayOutputStream()
        bmp.compress(CompressFormat.JPEG, 100, bos)
        return bos.toByteArray()
    }

    /**
     * 缩放位图到指定尺寸，维持宽高比
     *
     * @param bitmap       待缩放位图
     * @param targetWidth  目标缩放宽度
     * @param targetHeight 目标缩放高度
     * @return 缩放后的位图，可能返回null
     */
    fun scaleImageToFitSize(bitmap: Bitmap?, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (bitmap == null || targetWidth <= 0 || targetHeight <= 0) {
            return null
        }
        val origWidth = bitmap.width
        val origHeight = bitmap.height
        if (origWidth <= targetWidth && origHeight <= targetHeight) {
            return bitmap
        }
        val newWidth: Int
        val newHeight: Int
        if (origWidth > targetWidth && origHeight > targetHeight) {
            val scaleX = targetWidth / origWidth.toFloat()
            val scaleY = targetHeight / origHeight.toFloat()
            if (scaleX < scaleY) {
                newWidth = targetWidth
                newHeight = (origHeight * scaleX).toInt()
            } else {
                newHeight = targetHeight
                newWidth = (origWidth * scaleY).toInt()
            }
        } else if (origHeight > targetHeight) {
            val scale = targetHeight / origHeight.toFloat()
            newHeight = targetHeight
            newWidth = (origWidth * scale).toInt()
        } else {
            val scale = targetWidth / origWidth.toFloat()
            newWidth = targetWidth
            newHeight = (origHeight * scale).toInt()
        }
        var scaledBitmap: Bitmap? = null
        try {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (oom: OutOfMemoryError) {
            e(TAG, oom.message, oom)
            System.gc()
            try {
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } catch (e: Throwable) {
                e(TAG, e.message, e)
            }
        }
        return scaledBitmap
    }

    fun getBitmpaFromDrawable(context: Context, drawableRes: Int): Bitmap? {
        val res = context.resources
        var bmp: Bitmap? = null
        try {
            bmp = BitmapFactory.decodeResource(res, drawableRes)
        } catch (throwable: Throwable) {
            e(TAG, throwable.message, throwable)
        }
        return bmp
    }

    fun getWith(fixHeight: Int, bitmapWith: Int, bitmapHeight: Int): Int {
        return (bitmapWith / (bitmapHeight * 1.0f) * fixHeight).toInt()
    }

    fun getHeight(fixWith: Int, bitmapWith: Int, bitmapHeight: Int): Int {
        return (bitmapHeight / (bitmapWith * 1.0f) * fixWith).toInt()
    }
}