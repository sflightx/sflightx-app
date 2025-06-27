package com.sflightx.app.bottomsheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalBottomSheetHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bottomSheetController = remember { BottomSheetController() }
    rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true },
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            // Empty implementation; Compose handles basic nested scrolling
            // Customize if you need specific scroll behavior
        }
    }

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
                onDismissRequest = {
                    // Hide the sheet via controller
                    bottomSheetController.hide()
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = modifier
                    .nestedScroll(nestedScrollConnection)
                    .fillMaxWidth(),
            ) {
                sheetContent()
            }
        }

        content()
    }
}
