package com.chenyang.cywms.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView

private const val TAG_IME_ALLOWED = "cywms_ime_allowed"

/**
 * 业务模块页默认禁止软键盘（扫码枪 / 物理键盘优先）。
 * 登录页不要使用。双击文本框见 [Modifier.imeOnDoubleTap]。
 */
@Composable
fun ModuleSoftKeyboardGuard(enabled: Boolean = true) {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()

    DisposableEffect(enabled, view) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }
        val window = activity?.window
        val previousMode = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        fun harden(root: View) {
            if (root is EditText) {
                val allowed = root.tag == TAG_IME_ALLOWED
                root.showSoftInputOnFocus = allowed
                if (!allowed) root.hideIme()
            } else if (root is ViewGroup) {
                for (i in 0 until root.childCount) {
                    harden(root.getChildAt(i))
                }
            }
        }

        val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus is EditText) {
                val allowed = newFocus.tag == TAG_IME_ALLOWED
                newFocus.showSoftInputOnFocus = allowed
                if (!allowed) newFocus.hideIme()
            }
            harden(view.rootView)
        }

        harden(view.rootView)
        view.rootView.hideIme()
        view.viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalFocusChangeListener(focusListener)
            if (previousMode != null) {
                window?.setSoftInputMode(previousMode)
            }
        }
    }
}

/**
 * 双击文本框才允许弹出软键盘；单击只聚焦（不弹 IME）。
 * 用于模块页可见输入框。
 */
fun Modifier.imeOnDoubleTap(): Modifier = composed {
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    var lastTapMs by remember { mutableLongStateOf(0L) }

    this
        .onFocusChanged { state ->
            if (!state.isFocused) {
                (view.findFocus() as? EditText)?.let { et ->
                    if (et.tag == TAG_IME_ALLOWED) {
                        et.tag = null
                        et.showSoftInputOnFocus = false
                    }
                }
            } else {
                val et = view.findFocus() as? EditText
                if (et?.tag != TAG_IME_ALLOWED) {
                    et?.showSoftInputOnFocus = false
                    keyboard?.hide()
                    et?.hideIme()
                }
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    val now = System.currentTimeMillis()
                    val et = view.findFocus() as? EditText
                        ?: view.rootView.findDescendantEditText()
                    if (now - lastTapMs <= 350L) {
                        et?.tag = TAG_IME_ALLOWED
                        et?.showSoftInputOnFocus = true
                        et?.requestFocus()
                        keyboard?.show()
                    } else {
                        et?.tag = null
                        et?.showSoftInputOnFocus = false
                        keyboard?.hide()
                        et?.hideIme()
                    }
                    lastTapMs = now
                }
            )
        }
}

/** 扫码隐藏输入：可聚焦收键盘楔入，永不弹软键盘 */
fun Modifier.scanInputNoIme(): Modifier = composed {
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    this.onFocusChanged { state ->
        if (state.isFocused) {
            keyboard?.hide()
            (view.findFocus() as? EditText)?.let { et ->
                et.tag = null
                et.showSoftInputOnFocus = false
                et.hideIme()
            }
        }
    }
}

private fun View.hideIme() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

private fun View.findDescendantEditText(): EditText? {
    if (this is EditText) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).findDescendantEditText()?.let { return it }
        }
    }
    return null
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
