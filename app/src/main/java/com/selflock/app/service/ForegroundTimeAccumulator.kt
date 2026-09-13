package com.selflock.app.service

data class ForegroundInterval(val packageName: String, val seconds: Long)

class ForegroundTimeAccumulator(initialElapsedMillis: Long) {
    var currentPackage: String? = null
        private set

    private var lastObservationElapsed = initialElapsedMillis
    private var remainderMillis = 0L

    fun observe(packageName: String?, elapsedMillis: Long, shouldCount: Boolean): ForegroundInterval? {
        val previousPackage = currentPackage
        val elapsed = (elapsedMillis - lastObservationElapsed).coerceAtLeast(0L)
        val interval = if (previousPackage != null && shouldCount) {
            val totalMillis = remainderMillis + elapsed
            remainderMillis = totalMillis % 1000L
            ForegroundInterval(previousPackage, totalMillis / 1000L).takeIf { it.seconds > 0L }
        } else {
            remainderMillis = 0L
            null
        }
        if (previousPackage != packageName) remainderMillis = 0L
        currentPackage = packageName
        lastObservationElapsed = elapsedMillis
        return interval
    }
}
