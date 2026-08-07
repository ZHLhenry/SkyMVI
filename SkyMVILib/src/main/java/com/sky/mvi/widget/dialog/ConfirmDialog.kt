package com.sky.mvi.widget.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/**
 * 通用确认对话框。由 [visible] 控制显隐，避免每个页面手写 AlertDialog 样板。
 *
 * ```
 * var show by remember { mutableStateOf(false) }
 * ConfirmDialog(
 *     visible = show,
 *     message = "确定删除？",
 *     onConfirm = { show = false },
 *     onDismiss = { show = false }
 * )
 * ```
 */
@Composable
fun ConfirmDialog(
    visible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = modifier,
            title = title?.let { { Text(it) } },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(confirmText) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(dismissText) }
            },
            properties = DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                dismissOnClickOutside = dismissOnClickOutside
            )
        )
    }
}
