package com.example.englishforum.core.image

import android.net.Uri
import androidx.core.net.toUri
import java.io.File

/**
 * Defines constraints for resizing and compressing an image before upload.
 */
data class ImageProcessingConstraints(
    val maxDimension: Int,
    val quality: Int,
    val stripMetadata: Boolean = true
)

sealed interface ImageProcessingTarget {
    val constraints: ImageProcessingConstraints

    data object PostImage : ImageProcessingTarget {
        override val constraints: ImageProcessingConstraints = ImageProcessingConstraints(
            maxDimension = 1920,
            quality = 80
        )
    }

    data object Avatar : ImageProcessingTarget {
        override val constraints: ImageProcessingConstraints = ImageProcessingConstraints(
            maxDimension = 512,
            quality = 80
        )
    }
}

/**
 * Resulting file after processing an image for upload.
 */
data class ProcessedImage(
    val originalUri: Uri,
    val outputFile: File,
    val mimeType: String,
    val width: Int,
    val height: Int
) {
    val sizeBytes: Long get() = outputFile.length()
    val previewUri: Uri get() = outputFile.toUri()
}

interface ImageProcessor {
    suspend fun process(
        uri: Uri,
        target: ImageProcessingTarget
    ): Result<ProcessedImage>
}
