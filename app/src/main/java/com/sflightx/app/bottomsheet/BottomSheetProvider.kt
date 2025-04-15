package com.sflightx.app.bottomsheet

import androidx.compose.runtime.compositionLocalOf

val LocalBottomSheetController = compositionLocalOf<BottomSheetController> {
    error("BottomSheetController not initialized")
}
