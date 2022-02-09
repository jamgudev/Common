package com.jamgu.common.util.file

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.jamgu.common.util.log.JLog.e
import com.jamgu.common.util.file.security.SecurityUtil
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.NoSuchAlgorithmException
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {
    private const val TAG = "FileUtils"
    private const val ZIP_BUFFER_SIZE = 4 * 1024

    /**
     * Simple file comparator which only depends on file length and modification time.
     */
    private val SIMPLE_COMPARATOR: FileComparator = object : FileComparator {
        override fun equals(lhs: File, rhs: File): Boolean {
            return lhs.length() == rhs.length() && lhs.lastModified() == rhs.lastModified()
        }
    }

    /**
     * Copy files. If src is a directory, then all it's sub files will be copied into directory dst. If src is a file, then it will be copied to file
     * dst.
     *
     * @param src file or directory to copy.
     * @param dst destination file or directory.
     * @return true if copy complete perfectly, false otherwise (more than one file cannot be copied).
     */
    @JvmOverloads
    fun copyFiles(
        src: File?,
        dst: File?,
        filter: FileFilter? = null,
        comparator: FileComparator? = SIMPLE_COMPARATOR
    ): Boolean {
        if (src == null || dst == null) {
            return false
        }
        if (!src.exists()) {
            return false
        }
        if (src.isFile) {
            return performCopyFile(src, dst, filter, comparator)
        }
        val paths = src.listFiles() ?: return false
        // default is true.
        var result = true
        for (sub in paths) {
            if (!copyFiles(sub, File(dst, sub.name), filter)) {
                result = false
            }
        }
        return result
    }

    /**
     * 单个文件拷贝。
     */
    @Throws(IOException::class)
    fun copyFile(
        srcFilename: String?, destFilename: String?,
        overwrite: Boolean
    ) {
        srcFilename ?: return
        destFilename ?: return

        val srcFile = File(srcFilename)
        // 首先判断源文件是否存在
        if (!srcFile.exists()) {
            throw FileNotFoundException(
                "Cannot find the source file: "
                        + srcFile.absolutePath
            )
        }
        // 判断源文件是否可读
        if (!srcFile.canRead()) {
            throw IOException(
                "Cannot read the source file: "
                        + srcFile.absolutePath
            )
        }
        val destFile = File(destFilename)
        if (!overwrite) {
            // 目标文件存在就不覆盖
            if (destFile.exists()) return
        } else {
            // 如果要覆盖已经存在的目标文件，首先判断是否目标文件可写。
            if (destFile.exists()) {
                if (!destFile.canWrite()) {
                    throw IOException(
                        "Cannot write the destination file: "
                                + destFile.absolutePath
                    )
                }
            } else {
                // 不存在就创建一个新的空文件。
                if (!destFile.createNewFile()) {
                    throw IOException(
                        "Cannot write the destination file: "
                                + destFile.absolutePath
                    )
                }
            }
        }
        val block = ByteArray(1024)
        BufferedInputStream(FileInputStream(srcFile)).use { inputStream ->
            BufferedOutputStream(FileOutputStream(destFile)).use { outputStream ->
                while (true) {
                    val readLength = inputStream.read(block)
                    if (readLength == -1) break // end of file
                    outputStream.write(block, 0, readLength)
                }
            }
        }
    }

    fun getDirSize(file: File): Long {
        return if (file.exists()) {
            if (file.isDirectory) {
                val children = file.listFiles()
                var size: Long = 0
                if (children != null) {
                    for (f in children) {
                        size += getDirSize(f)
                    }
                }
                size
            } else {
                file.length()
            }
        } else 0
    }

    private fun performCopyFile(
        srcFile: File?,
        dstFile: File?,
        filter: FileFilter?,
        comparator: FileComparator?
    ): Boolean {
        if (srcFile == null || dstFile == null) {
            return false
        }
        if (filter != null && !filter.accept(srcFile)) {
            return false
        }
        try {
            if (!srcFile.exists() || !srcFile.isFile) {
                return false
            }
            if (dstFile.exists()) {
                if (comparator != null && comparator.equals(srcFile, dstFile)) {
                    // equal files.
                    return true
                } else {
                    // delete it in case of folder.
                    delete(dstFile)
                }
            }
            val toParent = dstFile.parentFile ?: return false
            if (toParent.isFile) {
                delete(toParent)
            }
            if (!toParent.exists() && !toParent.mkdirs()) {
                return false
            }
            FileInputStream(srcFile).channel.use { inc ->
                FileOutputStream(dstFile).channel.use { ouc ->
                    ouc.transferFrom(inc, 0, inc.size())
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            // exception occur, delete broken file.
            delete(dstFile)
            return false
        }
        return true
    }

    /**
     * Copy asset files. If assetName is a directory, then all it's sub files will be copied into directory dst. If assetName is a file, the it will
     * be copied to file dst.
     *
     * @param context   application context.
     * @param assetName asset name to copy.
     * @param dst       destination file or directory.
     */
    fun copyAssets(context: Context, assetName: String?, dst: String) {
        var assetName = assetName
        if (isEmpty(dst)) {
            return
        }
        if (assetName == null) {
            assetName = ""
        }
        val assetManager = context.assets
        var files: Array<String>? = null
        try {
            files = assetManager.list(assetName)
        } catch (e: FileNotFoundException) {
            // should be file.
            if (assetName.isNotEmpty()) {
                performCopyAssetsFile(context, assetName, dst)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (files == null) {
            return
        }
        if (files.isEmpty()) {
            // should be file or empty dir. Try to copy it.
            if (assetName.isNotEmpty()) {
                performCopyAssetsFile(context, assetName, dst)
            }
        }
        for (file in files) {
            if (isEmpty(file)) continue
            val newAssetDir = if (assetName.isEmpty()) file else assetName + File.separator + file
            val newDestDir = dst + File.separator + file
            copyAssets(context, newAssetDir, newDestDir)
        }
    }

    private fun performCopyAssetsFile(context: Context, assetPath: String, dstPath: String) {
        if (isEmpty(assetPath) || isEmpty(dstPath)) {
            return
        }
        val assetManager = context.assets
        val dstFile = File(dstPath)
        try {
            if (dstFile.exists()) {
                // try to determine whether or not copy this asset file, using their size.
                var tryStream = false
                try {
                    val fd = assetManager.openFd(assetPath)
                    if (dstFile.length() == fd.length) {
                        // same file already exists.
                        return
                    } else {
                        if (dstFile.isDirectory) {
                            delete(dstFile)
                        }
                    }
                } catch (e: IOException) {
                    // this file is compressed. cannot determine it's size.
                    tryStream = true
                }
                if (tryStream) {
                    assetManager.open(assetPath).use { tmpIn ->
                        if (dstFile.length() == tmpIn.available().toLong()) {
                            return
                        } else {
                            if (dstFile.isDirectory) {
                                delete(dstFile)
                            }
                        }
                    }
                }
            }
            val parent = dstFile.parentFile ?: return
            if (parent.isFile) {
                delete(parent)
            }
            if (!parent.exists() && !parent.mkdirs()) {
                return
            }
            assetManager.open(assetPath).use { ins ->
                BufferedOutputStream(FileOutputStream(dstFile)).use { out ->
                    val buf = ByteArray(1024)
                    var len: Int
                    while (ins.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            // delete broken file.
            delete(dstFile)
        }
    }
    /**
     * Delete corresponding path, file or directory.
     *
     * @param file      path to delete.
     * @param ignoreDir whether ignore directory. If true, all files will be deleted while directories is reserved.
     */
    /**
     * Delete corresponding path, file or directory.
     *
     * @param file path to delete.
     */
    @JvmOverloads
    fun delete(file: File?, ignoreDir: Boolean = false) {
        if (file == null || !file.exists()) {
            return
        }
        if (file.isFile) {
            val isDeleteSuccess = file.delete()
            if (!isDeleteSuccess) {
                Log.d("FileUtils", "delete() delete failed")
            }
            return
        }
        val fileList = file.listFiles() ?: return
        for (f in fileList) {
            delete(f, ignoreDir)
        }
        // delete the folder if need.
        if (!ignoreDir) {
            val isDeleteSuccess = file.delete()
            if (!isDeleteSuccess) {
                Log.d("FileUtils", "ignoreDir = false ,delete() delete failed")
            }
        }
    }

    private fun isEmpty(str: String?): Boolean {
        return str == null || str.isEmpty()
    }

    fun zip(srcFiles: Array<File?>?, dest: FileOutputStream?): Boolean {
        // 参数检查
        if (srcFiles == null || srcFiles.isEmpty() || dest == null) {
            return false
        }
        var resu: Boolean
        ZipOutputStream(BufferedOutputStream(dest)).use {
            resu = try {
                val buffer = ByteArray(ZIP_BUFFER_SIZE)
                // 添加文件到ZIP压缩流
                for (src in srcFiles) {
                    doZip(it, src, null, buffer)
                }
                it.flush()
                it.closeEntry()
                true
            } catch (e: Exception) {
                // e.print*StackTrace();
                false
            }

        }
        return resu
    }

    /**
     * ZIP压缩多个文件/文件夹
     *
     * @param srcFiles 要压缩的文件/文件夹列表
     * @param dest     目标文件
     * @return 压缩成功/失败
     */
    fun zip(srcFiles: Array<File?>?, dest: File?): Boolean {
        try {
            return zip(srcFiles, FileOutputStream(dest))
        } catch (e: FileNotFoundException) {
            e("FileUtils", e.message, e)
        }
        return false
    }

    /**
     * 方法：ZIP压缩单个文件/文件夹
     *
     * @param src  源文件/文件夹
     * @param dest 目标文件
     * @return 压缩成功/失败
     */
    fun zip(src: File?, dest: File?): Boolean {
        return zip(arrayOf(src), dest)
    }

    /**
     * 方法：解压缩单个ZIP文件
     *
     * @param src        源文件/文件夹
     * @param destFolder 目标文件夹
     * @return 解压缩成功/失败
     */
    fun unzip(src: File?, destFolder: File): Boolean {
        if (src == null || src.length() < 1 || !src.canRead()) {
            return false
        }
        var resu = false
        if (!destFolder.exists()) {
            destFolder.mkdirs()
        }
        val buffer = ByteArray(8 * 1024)
        var readLen: Int
        ZipInputStream(FileInputStream(src)).use { zis ->
            try {
                var entry: ZipEntry
                while (null != zis.nextEntry.also { entry = it }) {
                    println(entry.name)
                    if (entry.name.startsWith("../")) {
                        break
                    }
                    if (entry.isDirectory) {
                        File(destFolder, entry.name).mkdirs()
                    } else {
                        val entryFile = File(destFolder, entry.name)
                        entryFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(entryFile)).use { bos ->
                            while (-1 != zis.read(buffer, 0, buffer.size).also { readLen = it }) {
                                bos.write(buffer, 0, readLen)
                            }
                            bos.flush()
                        }
                    }
                }
                zis.closeEntry()
                resu = true
            } catch (e: Exception) {
                resu = false
            }
        }
        return resu
    }

    /**
     * 压缩文件/文件夹到ZIP流中 <br></br> <br></br> *本方法是为了向自定义的压缩流添加文件/文件夹，若只是要压缩文件/文件夹到指定位置，请使用 `FileUtils.zip()` 方法*
     *
     * @param zos    ZIP输出流
     * @param file   被压缩的文件
     * @param root   被压缩的文件在ZIP文件中的入口根节点
     * @param buffer 读写缓冲区
     * @throws IOException 读写流时可能抛出的I/O异常
     */
    @Throws(IOException::class)
    fun doZip(zos: ZipOutputStream?, file: File?, root: String?, buffer: ByteArray) {
        // 参数检查
        if (zos == null || file == null) {
            throw IOException("I/O Object got NullPointerException")
        }
        if (!file.exists()) {
            throw FileNotFoundException("Target File is missing")
        }
        var readLen = 0
        val rootName = if (TextUtils.isEmpty(root)) file.name else root + File.separator + file.name

        // 文件直接放入压缩流中
        if (file.isFile) {
            BufferedInputStream(FileInputStream(file)).use { bis ->
                try {
                    zos.putNextEntry(ZipEntry(rootName))
                    while (-1 != bis.read(buffer, 0, buffer.size).also { readLen = it }) {
                        zos.write(buffer, 0, readLen)
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
        } else if (file.isDirectory) {
            val subFiles = file.listFiles()
            if (subFiles != null) {
                for (subFile in subFiles) {
                    doZip(zos, subFile, rootName, buffer)
                }
            }
        }
    }

    fun unjar(src: File?, destFolder: File): Boolean {
        if (src == null || src.length() < 1 || !src.canRead()) {
            return false
        }
        var resu: Boolean
        if (!destFolder.exists()) {
            destFolder.mkdirs()
        }
        val buffer = ByteArray(8 * 1024)
        var readLen: Int
        JarInputStream(FileInputStream(src)).use { zis ->
            try {
                var entry: JarEntry
                while (null != zis.nextJarEntry.also { entry = it }) {
                    println(entry.name)
                    if (entry.name.startsWith("../")) {
                        break
                    }
                    if (entry.isDirectory) {
                        File(destFolder, entry.name).mkdirs()
                    } else {
                        BufferedOutputStream(FileOutputStream(File(destFolder, entry.name))).use { bos ->
                            while (-1 != zis.read(buffer, 0, buffer.size).also { readLen = it }) {
                                bos.write(buffer, 0, readLen)
                            }
                            bos.flush()
                        }
                    }
                }
                zis.closeEntry()
                resu = true
            } catch (e: java.lang.Exception) {
                resu = false
            }
        }
        return resu
    }

    /**
     * 此文件是否存在
     */
    fun isExistFile(uploadFilePath: String?): Boolean {
        uploadFilePath ?: return false

        if (TextUtils.isEmpty(uploadFilePath)) {
            //文件不存在 
            return false
        }
        try {
            val file = File(uploadFilePath)
            if (!file.exists() || !file.isFile || file.length() == 0L) {
                return false
            }
        } catch (e: Exception) {
            e("UploadTask", e.message, e)
            return false
        }
        return true
    }

    @Throws(IOException::class)
    fun createFile(filePath: String?): Boolean {
        filePath ?: return false
        return create(File(filePath))
    }

    /**
     * 创建文件，包括必要的父目录的创建，如果未创建
     *
     * @param file 待创建的文件
     * @return 返回操作结果
     * @throws IOException 创建失败，将抛出该异常
     */
    @Throws(IOException::class)
    fun create(file: File): Boolean {
        if (file.exists()) {
            return true
        }
        val parent = file.parentFile ?: return false
        val flag = parent.mkdirs()
        if (!flag) {
            e(TAG, "FileUtils unCompress mkdirs fail . create :" + parent.absolutePath)
        }
        return file.createNewFile()
    }

    /**
     * 移动文件
     *
     * @param srcPath  源文件路径
     * @param destPath 目标文件路径
     * @return 返回操作结果
     */
    fun moveFile(srcPath: String?, destPath: String?): Boolean {
        val src = File(srcPath)
        val dest = File(destPath)
        val ret = copyFiles(src, dest)
        if (ret) {
            deleteFile(srcPath)
        }
        return ret
    }

    /**
     * 移动文件
     *
     * @param src  源文件
     * @param dest 目标文件
     * @return 返回操作结果
     */
    fun moveFile(src: File, dest: File?): Boolean {
        var ret = copyFiles(src, dest)
        if (ret) {
            ret = deleteFile(src.absolutePath)
        }
        return ret
    }

    /**
     * 将数据写入一个文件
     *
     * @param destFilePath 要创建的文件的路径
     * @param data         待写入的文件数据
     * @param startPos     起始偏移量
     * @param length       要写入的数据长度
     * @return 成功写入文件返回true, 失败返回false
     */
    fun writeFile(destFilePath: String?, data: ByteArray?, startPos: Int, length: Int): Boolean {
        try {
            if (!createFile(destFilePath)) {
                return false
            }
            FileOutputStream(destFilePath).use { fos ->
                fos.write(data, startPos, length)
            }
            return true
        } catch (e: FileNotFoundException) {
            e(TAG, e.message, e)
        } catch (e: IOException) {
            e(TAG, e.message, e)
        }
        return false
    }

    /**
     * 从一个输入流里写文件,该类还需要优化
     *
     * @param destFilePath 要创建的文件的路径
     * @param in           要读取的输入流
     * @return 写入成功返回true, 写入失败返回false
     */
    fun writeFile(destFilePath: String?, `in`: InputStream): Boolean {
        try {
            if (!createFile(destFilePath)) {
                return false
            }
            FileOutputStream(destFilePath).use { fos ->
                var readCount: Int
                val len = 8192
                val buffer = ByteArray(len)
                while (`in`.read(buffer).also { readCount = it } != -1) {
                    fos.write(buffer, 0, readCount)
                }
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 删除文件或者目录
     *
     * @param path 指定路径的文件或目录
     * @return 返回操作结果
     */
    fun deleteFile(path: String?): Boolean {
        path ?: return false

        val file = File(path)
        if (!file.exists()) return true
        delete(file)
        return true
    }

    /**
     * 创建目录，包括必要的父目录的创建，如果未创建
     *
     * @param path 待创建的目录路径
     * @return 返回操作结果
     */
    fun mkdir(path: String?): Boolean {
        path ?: return false
        val file = File(path)
        if (file.exists() && file.isDirectory) {
            return true
        }
        val flag = file.mkdirs()
        if (!flag) {
            e(TAG, "FileUtils unCompress mkdir fail . create :" + file.absolutePath)
        }
        return true
    }

    /**
     * 复制文件
     *
     * @param srcPath  源文件路径
     * @param destPath 目标文件路径
     * @return 返回操作结果
     */
    fun copyFile(srcPath: String?, destPath: String?): Boolean {
        if (!srcPath.isNullOrEmpty() && !destPath.isNullOrEmpty()) {
            return copyFiles(File(srcPath), File(destPath))
        }
        return false
    }

    /**
     * 判断文件是否存在
     *
     * @param filePath 路径名
     * @return
     */
    fun isFileExist(filePath: String?): Boolean {
        filePath ?: return false

        val file = File(filePath)
        return file.exists()
    }

    fun copyStream(src: InputStream, dest: OutputStream): Boolean {
        try {
            src.use {
                val buffer = ByteArray(2048)
                var bytesread: Int
                while (src.read(buffer).also { bytesread = it } != -1) {
                    if (bytesread > 0) dest.use { dest.write(buffer, 0, bytesread) }
                }
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun readFile(filePath: String?): ByteArray? {
        filePath ?: return null

        return if (isFileExist(filePath)) {
            try {
                readFile(File(filePath))
            } catch (e: Exception) {
                ByteArray(0)
            }
        } else ByteArray(0)
    }

    /**
     * 读取文件内容
     *
     * @param file 文件句柄对象
     * @return 返回读取的字节数组
     * @throws IOException 读取失败将抛出该异常
     */
    @Throws(IOException::class)
    fun readFile(file: File): ByteArray? {
        val len = file.length().toInt()
        if (len == 0) {
            return ByteArray(0)
        }
        var data: ByteArray?
        val fis = FileInputStream(file)
        BufferedInputStream(fis).use { bis ->
            data = ByteArray(len)
            bis.read(data)
        }
        return data
    }

    /**
     * 读取文件内容
     *
     * @param ins 文件句柄对象
     * @return 返回读取的字节数组
     * @throws IOException 读取失败将抛出该异常
     */
    @Throws(IOException::class)
    fun readFile(ins: InputStream?): ByteArray? {
        if (ins == null) {
            return ByteArray(0)
        }
        var data: ByteArray? = null
        BufferedInputStream(ins).use { bis ->
            data = ByteArray(ins.available())
            bis.read(data)
        }
        return data
    }

    /**
     * 保存Bitmap到文件系统
     *
     * @param bitmap
     * @param outputFile
     * @param format     图片文件压缩格式：默认JPEG
     * @param quality    图片压缩质量：取值0-100,默认100
     * @return
     */
    @Suppress("NAME_SHADOWING")
    fun saveImg(bitmap: Bitmap?, outputFile: File?, format: CompressFormat?, quality: Int): Boolean {
        var format = format
        var quality = quality
        if (bitmap == null || outputFile == null) return false
        var success = false
        try {
            val parentFile = outputFile.parentFile
            if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
                return false
            }
            FileOutputStream(outputFile.absolutePath).use { os ->
                format = format ?: CompressFormat.JPEG
                quality = if (quality in 0..100) quality else 100
                bitmap.compress(format, quality, os)
                success = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return success
    }

    /**
     * 获得文件夹内文件的个数。
     *
     * @param filePath
     * @return
     */
    fun getFileCount(filePath: String?): Long {
        filePath ?: return 0L

        val f = File(filePath)
        if (!f.exists()) {
            return 0
        }
        var count: Long = 0
        val fList = f.listFiles() ?: return 0L
        for (i in fList.indices) {
            count = if (fList[i].isDirectory) {
                count + getFileCount(fList[i].path)
            } else {
                count + 1
            }
        }
        return count
    }

    fun getFileMd5(path: String?): String? {
        if (TextUtils.isEmpty(path)) return null
        try {
            val file = File(path)
            if (file.exists()) return getFileMd5(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getFileMd5(file: File?): String {
        return SecurityUtil.encrypt(file)
    }

    fun getInputStreamMd5(`is`: InputStream?): String? {
        try {
            return SecurityUtil.encryptOrThrow(`is`)
        } catch (e: IOException) {
            e(TAG, e.message, e)
        } catch (e: NoSuchAlgorithmException) {
            e(TAG, e.message, e)
        }
        return null
    }

    // ------------- common --------------
    private val sCacheDirLock = Any()

    /**
     * Get common cache dir(external if available, or internal) with corresponding name, which is not persist.
     */
    private fun getCacheDir(context: Context, name: String): String {
        return getCacheDir(context, name, false)
    }

    /**
     * Get common cache dir(external if available, or internal) with corresponding name.
     *
     * @param context context
     * @param name    cache dir name.
     * @param persist whether this cache dir should be persist or not.
     * @return cache dir.
     */
    fun getCacheDir(context: Context, name: String, persist: Boolean): String {
        init(context)
        val dir = getExternalCacheDir(context, name, persist)
        return dir ?: getInternalCacheDir(context, name, persist)!!
    }

    /**
     * Get external cache dir with corresponding name, which is not persist.
     */
    fun getExternalCacheDir(context: Context, name: String): String? {
        return getExternalCacheDir(context, name, false)
    }

    /**
     * Get external cache dir with corresponding name.
     *
     * @param persist whether this cache dir should be persist or not.
     */
    fun getExternalCacheDir(context: Context, name: String, persist: Boolean): String? {
        init(context)
        val dir = getExternalCacheDir(context, persist)
            ?: return null
        if (isEmpty(name)) {
            return dir
        }
        val file = File(dir + File.separator + name)
        if (!file.exists() || !file.isDirectory) {
            synchronized(sCacheDirLock) {
                if (!file.isDirectory) {
                    delete(file)
                    file.mkdirs()
                } else if (!file.exists()) {
                    file.mkdirs()
                } else {
                    // do nothing
                }
            }
        }
        return file.absolutePath
    }

    fun getExternalCacheDir(context: Context, persist: Boolean): String? {
        init(context)
        if (!isExternalAvailable) {
            return null
        }
        val externalDir = if (!persist) InnerEnvironment.getExternalCacheDir(
            context,
            false
        ) else InnerEnvironment.getExternalFilesDir(context, null, false)
        return externalDir?.absolutePath
    }

    /**
     * Get extend external cache dir with corresponding name, which is not persist.
     */
    fun getExternalCacheDirExt(context: Context, name: String): String? {
        return getExternalCacheDirExt(context, name, false)
    }

    /**
     * Get extend external cache dir with corresponding name.
     *
     * @param persist whether this cache dir should be persist or not.
     */
    fun getExternalCacheDirExt(context: Context, name: String, persist: Boolean): String? {
        init(context)
        val dir = getExternalCacheDirExt(context, persist)
            ?: return null
        if (isEmpty(name)) {
            return dir
        }
        val file = File(dir + File.separator + name)
        if (!file.exists() || !file.isDirectory) {
            synchronized(sCacheDirLock) {
                if (!file.isDirectory) {
                    delete(file)
                    file.mkdirs()
                } else if (!file.exists()) {
                    file.mkdirs()
                } else {
                    // do nothing
                }
            }
        }
        return file.absolutePath
    }

    fun getExternalCacheDirExt(context: Context, persist: Boolean): String? {
        init(context)
        if (!isExternalAvailable) {
            return null
        }
        val externalDir =
            if (!persist) InnerEnvironment.getExternalCacheDir(context, true) else InnerEnvironment.getExternalFilesDir(
                context,
                null,
                true
            )
        return externalDir?.absolutePath
    }

    /**
     * Get internal cache dir with corresponding name, which is not persist.
     */
    fun getInternalCacheDir(context: Context, name: String): String? {
        return getInternalCacheDir(context, name, false)
    }

    /**
     * Get internal cache dir with corresponding name.
     *
     * @param persist whether this cache dir should be persist or not.
     */
    fun getInternalCacheDir(context: Context, name: String, persist: Boolean): String? {
        init(context)
        val dir = getInternalCacheDir(context, persist)
            ?: return null
        if (isEmpty(name)) {
            return dir
        }
        val file = File(dir + File.separator + name)
        if (!file.exists() || !file.isDirectory) {
            synchronized(sCacheDirLock) {
                if (!file.isDirectory) {
                    delete(file)
                    file.mkdirs()
                } else if (!file.exists()) {
                    file.mkdirs()
                } else {
                }
            }
        }
        return file.absolutePath
    }

    fun getInternalCacheDir(context: Context, persist: Boolean): String? {
        init(context)
        if (!persist) {
            val cacheDir = context.cacheDir
            if (cacheDir != null) {
                return cacheDir.absolutePath
            }
        } else {
            val filesDir = context.filesDir
            if (filesDir != null) {
                return filesDir.absolutePath + File.separator + "cache"
            }
        }
        return null
    }

    fun getInternalFileDir(context: Context, persist: Boolean): String {
        init(context)
        return if (!persist) context.cacheDir.absolutePath else context.filesDir.absolutePath + File.separator
    }

    /**
     * Determine whether a path is external.
     */
    fun isExternal(path: String?): Boolean {
        val externalCacheDir = Environment.getExternalStorageDirectory().absolutePath
        return path != null && path.startsWith(externalCacheDir)
    }

    /**
     * Determine whether a path is internal.
     */
    fun isInternal(path: String?): Boolean {
        val internalCacheDir = Environment.getDataDirectory().absolutePath
        return path != null && path.startsWith(internalCacheDir)
    }

    /**
     * Whether the external storage is available.
     */
    private val isExternalAvailable: Boolean
        get() = if (sHasRegister) {
            var state = mSdcardState
            if (state == null) {
                state = Environment.getExternalStorageState()
                mSdcardState = state
            }
            Environment.MEDIA_MOUNTED == state
        } else {
            Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        }

    // ---------------- sdcard state receiver ---------------------//
    @Volatile
    private var mSdcardState: String? = null
    private val mSdcardStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                mSdcardState = Environment.getExternalStorageState()
            } catch (e: Throwable) {
                e("FileUtils", e.message, e)
            }
        }
    }

    @Volatile
    private var sHasRegister = false
    private fun registerSdcardReceiver(context: Context) {
        try {
            if (!sHasRegister) {
                val filter = IntentFilter()
                filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL) // 扩展介质（扩展卡）已经从 SD卡插槽拔出，但是挂载点 (mount point)还没解除 (unmount)
                filter.addAction(Intent.ACTION_MEDIA_EJECT) // 用户想要移除扩展介质（拔掉扩展卡）
                filter.addAction(Intent.ACTION_MEDIA_MOUNTED) // 扩展介质被插入，而且已经被挂载
                filter.addAction(Intent.ACTION_MEDIA_REMOVED) // 扩展介质被移除。
                filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED) // 扩展介质存在，但是还没有被挂载(mount)
                filter.addDataScheme("file") // 这个很重要
                context.registerReceiver(mSdcardStateReceiver, filter)
                sHasRegister = true
            }
        } catch (e: Throwable) {
            e("FileUtils", "regist sdcard receiver failed. " + e.message, e)
        }
    }

    fun init(context: Context) {
        var context = context
        context = context.applicationContext
        registerSdcardReceiver(context)
    }

    /**
     * Comparator of files.
     */
    interface FileComparator {
        fun equals(lhs: File, rhs: File): Boolean
    }

    internal object InnerEnvironment {
        private const val TAG = "InnerEnvironment"
        private const val EXTEND_SUFFIX = "-ext"
        private val externalStorageAndroidDataDir = File(
            File(
                Environment.getExternalStorageDirectory(),
                "Android"
            ), "data"
        )

        fun getExternalStorageAppCacheDirectory(packageName: String?): File? {
            if (!packageName.isNullOrEmpty()) {
                return File(
                    File(
                        externalStorageAndroidDataDir,
                        packageName
                    ), "cache"
                )
            }
            return null
        }

        fun getExternalStorageAppFilesDirectory(packageName: String?): File? {
            if (!packageName.isNullOrEmpty()) return File(
                File(
                    externalStorageAndroidDataDir,
                    packageName
                ), "files"
            )

            return null
        }

        @SuppressLint("ObsoleteSdkInt")
        fun getExternalCacheDir(context: Context, extend: Boolean): File? {
            if (!extend && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                try {
                    return context.externalCacheDir
                } catch (e: Throwable) {
                    e(TAG, e.message, e)
                }
            }
            synchronized(InnerEnvironment::class.java) {
                val externalCacheDir = getExternalStorageAppCacheDirectory(
                    context.packageName + if (extend) EXTEND_SUFFIX else ""
                ) ?: return null
                if (!externalCacheDir.exists()) {
                    try {
                        val isCreateSuccuss = File(externalStorageAndroidDataDir, ".nomedia").createNewFile()
                        if (!isCreateSuccuss) {
                            Log.w(TAG, "Unable to create new file")
                        }
                    } catch (e: Throwable) {
                        e(TAG, e.message, e)
                    }
                    if (!externalCacheDir.mkdirs()) {
                        Log.w(TAG, "Unable to create external cache directory")
                        return null
                    }
                }
                return externalCacheDir
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        fun getExternalFilesDir(context: Context, type: String?, extend: Boolean): File? {
            if (!extend && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                return context.getExternalFilesDir(type)
            }
            synchronized(InnerEnvironment::class.java) {
                val externalFilesDir = getExternalStorageAppFilesDirectory(
                    context.packageName + if (extend) EXTEND_SUFFIX else ""
                ) ?: return null
                if (!externalFilesDir.exists()) {
                    try {
                        val isCreateSuccuss = File(externalStorageAndroidDataDir, ".nomedia").createNewFile()
                        if (!isCreateSuccuss) {
                            Log.w(TAG, "Unable to create nomedia file")
                        }
                    } catch (e: IOException) {
                    }
                    if (!externalFilesDir.mkdirs()) {
                        Log.w(TAG, "Unable to create external files directory")
                        return null
                    }
                }
                if (type == null) {
                    return externalFilesDir
                }
                val dir = File(externalFilesDir, type)
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.w(TAG, "Unable to create external media directory $dir")
                        return null
                    }
                }
                return dir
            }
        }
    }
}