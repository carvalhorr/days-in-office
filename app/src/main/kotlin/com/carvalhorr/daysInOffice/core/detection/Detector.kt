package com.carvalhorr.daysInOffice.core.detection

import android.content.Context
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig

interface Detector {
    suspend fun isAtOffice(config: DetectionConfig): Boolean
    suspend fun isAvailable(context: Context, config: DetectionConfig): Boolean
}
