package com.example.englishforum.core.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultImageProcessor(
    context: Context,
    private val contentResolver: ContentResolver = context.contentResolver,
    private val cacheDir: File = context.cacheDir,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ImageProcessor {

    override suspend fun process(
        uri: Uri,
        target: ImageProcessingTarget
    ): Result<ProcessedImage> = withContext(ioDispatcher) {
        runCatching { processInternal(uri, target.constraints) }
            .fold(
                onSuccess = { processed ->
                    Result.success(processed)
                },
                onFailure = { throwable ->
                    Log.w(TAG, "Failed to process image uri=$uri", throwable)
                    Result.failure(mapFailure(throwable))
                }
            )
    }

    private fun processInternal(
        uri: Uri,
        constraints: ImageProcessingConstraints
    ): ProcessedImage {
        val orientation = resolveOrientation(uri)
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(uri, orientation, constraints.maxDimension)
        } else {
            val bounds = decodeBounds(uri)
            val (initialWidth, initialHeight) = bounds ?: throw ImageProcessingFailure.InvalidImage
            val (correctedWidth, correctedHeight) = applyOrientationToDimensions(
                initialWidth,
                initialHeight,
                orientation
            )
            val sampleSize = calculateInSampleSize(
                width = correctedWidth,
                height = correctedHeight,
                maxDimension = constraints.maxDimension
            )
            val decoded = decodeBitmap(uri, sampleSize)
                ?: throw ImageProcessingFailure.InvalidImage
            val rotated = rotateBitmapIfNeeded(decoded, orientation)
            val resized = resizeBitmapIfNeeded(rotated, constraints.maxDimension)
            if (resized !== rotated && !rotated.isRecycled) {
                rotated.recycle()
            }
            if (rotated !== decoded && !decoded.isRecycled) {
                decoded.recycle()
            }
            resized
        }

        val finalWidth = bitmap.width
        val finalHeight = bitmap.height

        val compressFormat = resolveCompressFormat(bitmap)
        val mimeType = compressFormat.toMimeType()
        val outputFile = createOutputFile(compressFormat)

        outputFile.outputStream().use { stream ->
            val didCompress = bitmap.compress(
                compressFormat,
                constraints.quality,
                stream
            )
            if (!didCompress) {
                throw ImageProcessingFailure.ProcessingError()
            }
        }

        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }

        return ProcessedImage(
            originalUri = uri,
            outputFile = outputFile,
            mimeType = mimeType,
            width = finalWidth,
            height = finalHeight
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(
        uri: Uri,
        orientation: Int,
        maxDimension: Int
    ): Bitmap {
        val source = ImageDecoder.createSource(contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (correctedWidth, correctedHeight) = applyOrientationToDimensions(
                info.size.width,
                info.size.height,
                orientation
            )
            val longerEdge = maxOf(correctedWidth, correctedHeight)
            if (longerEdge > 0) {
                val scale = if (longerEdge > maxDimension) {
                    maxDimension.toFloat() / longerEdge.toFloat()
                } else {
                    1f
                }
                val targetWidth = (correctedWidth * scale).toInt().coerceAtLeast(1)
                val targetHeight = (correctedHeight * scale).toInt().coerceAtLeast(1)
                decoder.setTargetSize(targetWidth, targetHeight)
            }
            decoder.isMutableRequired = false
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun decodeBounds(uri: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw ImageProcessingFailure.SourceUnavailable
        val width = options.outWidth
        val height = options.outHeight
        return if (width > 0 && height > 0) width to height else null
    }

    private fun resolveOrientation(uri: Uri): Int {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun decodeBitmap(uri: Uri, sampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw ImageProcessingFailure.SourceUnavailable
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotationDegrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longerEdge = maxOf(width, height)
        if (longerEdge <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / longerEdge.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun resolveCompressFormat(bitmap: Bitmap): Bitmap.CompressFormat {
        return if (bitmap.hasAlpha()) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
    }

    private fun createOutputFile(format: Bitmap.CompressFormat): File {
        val extension = when (format) {
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "img"
        }
        return File.createTempFile("ef_image_", ".${extension}", cacheDir)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxDimension: Int
    ): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height

        while (maxOf(currentWidth, currentHeight) / 2 >= maxDimension) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sampleSize
    }

    private fun applyOrientationToDimensions(
        width: Int,
        height: Int,
        orientation: Int
    ): Pair<Int, Int> {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE -> height to width
            else -> width to height
        }
    }

    private fun mapFailure(throwable: Throwable): Throwable {
        return when (throwable) {
            is ImageProcessingFailure -> throwable
            is IOException -> ImageProcessingFailure.SourceUnavailable
            is OutOfMemoryError -> ImageProcessingFailure.ProcessingError(throwable)
            else -> ImageProcessingFailure.ProcessingError(throwable)
        }
    }

    private fun Bitmap.CompressFormat.toMimeType(): String {
        return when (this) {
            Bitmap.CompressFormat.PNG -> "image/png"
            Bitmap.CompressFormat.WEBP -> "image/webp"
            else -> "image/jpeg"
        }
    }

    companion object {
        private const val TAG = "ImageProcessor"
    }
}
