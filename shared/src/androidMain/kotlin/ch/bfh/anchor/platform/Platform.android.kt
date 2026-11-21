package ch.bfh.anchor.platform

import java.util.UUID

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun randomId(): String = UUID.randomUUID().toString()

actual val isIosHost: Boolean = false
