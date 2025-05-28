package com.axerioo.gamesphere.data.remote.utils

// --- IgdbImageUtil Object ---
// A utility object for constructing image URLs for the IGDB API.

object IgdbImageUtil {

    // --- ImageSize Enum ---
    // Defines various standard image sizes provided by the IGDB API.

    enum class ImageSize(val key: String) {
        COVER_SMALL("cover_small"),             // 90 x 128 (Fit)
        SCREENSHOT_MED("screenshot_med"),       // 569 x 320 (Lfill, Center gravity)
        COVER_BIG("cover_big"),                 // 264 x 374 (Fit)
        LOGO_MED("logo_med"),                   // 284 x 160 (Fit)
        SCREENSHOT_BIG("screenshot_big"),       // 889 x 500 (Lfill, Center gravity)
        SCREENSHOT_HUGE("screenshot_huge"),     // 1280 x 720 (Lfill, Center gravity)
        THUMB("thumb"),                         // 90 x 90 (Thumb, Center gravity)
        MICRO("micro"),                         // 35 x 35 (Thumb, Center gravity)
        HD("720p"),                             // 1280 x 720 (Fit, Center gravity)
        FULL_HD("1080p")                        // 1920 x 1080 (Fit, Center gravity)
    }

    // --- getImageUrl Function ---
    // Constructs a full image URL given an `imageId` and an `ImageSize`.

    fun getImageUrl(imageId: String?, size: ImageSize = ImageSize.COVER_BIG): String? {
        return imageId?.let {
            "https://images.igdb.com/igdb/image/upload/t_${size.key}/$it.jpg"
        }
    }
}