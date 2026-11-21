package ch.bfh.anchor.platform

/** Wall-clock milliseconds since the Unix epoch. */
expect fun nowMillis(): Long

/** A lowercase random UUID string, matching the ids the Android app already wrote. */
expect fun randomId(): String

/** True when running on iOS. Read through `LocalIsIos` in UI code so previews can override it. */
expect val isIosHost: Boolean
