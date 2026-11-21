package ch.bfh.anchor.platform

import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun randomId(): String = NSUUID().UUIDString().lowercase()

actual val isIosHost: Boolean = true
