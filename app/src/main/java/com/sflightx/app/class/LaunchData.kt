package com.sflightx.app.`class`

import androidx.annotation.*
import java.time.*
import java.time.format.*

@Keep
data class LaunchData(
    val name: String = "",
    val description: String = "",
    val postKey: String = "",
    val thumbnail: String = "",
    val key: String = "",
    val companyId: String = "",
    val statusId: String = "",
    val vehicleId: String = "",
    val recovery: Any = emptyList<Any>(),  // Change to Any to accommodate both List and Map
    val payload: Any = emptyList<Any>(),  // Same here for payload
    val link: Any = emptyMap<String, Any>(), // Handle link as Map<String, Any>
    val launch_site: LaunchSite = LaunchSite(),
    val net: LaunchNet = LaunchNet(),
)

@Keep
data class LaunchSite(
    val pad: String = "",
    val address: String = "",
    val complex: String = ""
)

@Keep
data class LaunchNet(
    val start: Long = 0L,
    val end: Long = 0L
) {

    val startInstant: Instant get() = Instant.ofEpochMilli(start)
    val endInstant: Instant get() = Instant.ofEpochMilli(end)

    fun formatStart(pattern: String = "yyyy-MM-dd HH:mm z", zoneId: ZoneId = ZoneId.of("UTC")): String {
        return startInstant.atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern))
    }

    fun formatEnd(pattern: String = "yyyy-MM-dd HH:mm z", zoneId: ZoneId = ZoneId.of("UTC")): String {
        return endInstant.atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern))
    }
}
