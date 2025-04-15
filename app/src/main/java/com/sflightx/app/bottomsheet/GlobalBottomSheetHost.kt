package com.sflightx.app.bottomsheet

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalBottomSheetHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bottomSheetController = remember { BottomSheetController() }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false, // optional: force only expanded/collapsed
        confirmValueChange = { true },
    )

    CompositionLocalProvider(LocalBottomSheetController provides bottomSheetController) {
        val sheetContent = bottomSheetController.content

        LaunchedEffect(bottomSheetController.isVisible) {
            if (bottomSheetController.isVisible && sheetContent != null) {
                // Force expand immediately
                sheetState.show()
            } else {
                sheetState.hide()
            }
        }

        if (bottomSheetController.isVisible && sheetContent != null) {
            ModalBottomSheet(
                onDismissRequest = { bottomSheetController.hide() },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = modifier
            ) {
                sheetContent()
            }
        }

        content()
    }
}
