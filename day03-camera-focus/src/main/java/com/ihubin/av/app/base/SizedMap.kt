package com.ihubin.av.app.base

import android.util.ArrayMap
import java.util.*

class SizeMap {
    private val mRatios: ArrayMap<AspectRatio, SortedSet<Size>> = ArrayMap()

    /**
     * Add a new [Size] to this collection.
     *
     * @param size The size to add.
     * @return `true` if it is added, `false` if it already exists and is not added.
     */
    fun add(size: Size): Boolean {
        for (ratio in mRatios.keys) {
            if (ratio.matches(size)) {
                val sizes: SortedSet<Size> = mRatios[ratio]!!
                return if (sizes.contains(size)) {
                    false
                } else {
                    sizes.add(size)
                    true
                }
            }
        }
        // None of the existing ratio matches the provided size; add a new key
        val sizes: SortedSet<Size> = TreeSet()
        sizes.add(size)
        mRatios[AspectRatio.of(size.width, size.height)] = sizes
        return true
    }

    /**
     * Removes the specified aspect ratio and all sizes associated with it.
     *
     * @param ratio The aspect ratio to be removed.
     */
    fun remove(ratio: AspectRatio?) {
        mRatios.remove(ratio)
    }

    fun ratios(): Set<AspectRatio> {
        return mRatios.keys
    }

    fun sizes(ratio: AspectRatio): SortedSet<Size>? {
        return mRatios[ratio]
    }

    fun clear() {
        mRatios.clear()
    }

    val isEmpty: Boolean
        get() = mRatios.isEmpty()
}