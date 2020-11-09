package com.ihubin.av.app.base

import android.os.Parcel
import android.os.Parcelable
import androidx.collection.SparseArrayCompat

class AspectRatio private constructor(private val x: Int, val y: Int) : Comparable<AspectRatio>,
    Parcelable {

    fun matches(size: Size): Boolean {
        val gcd = gcd(
            size.width,
            size.height
        )
        val x: Int = size.width / gcd
        val y: Int = size.height / gcd
        return this.x == x && this.y == y
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this === other) {
            return true
        }
        if (other is AspectRatio) {
            return x == other.x && y == other.y
        }
        return false
    }

    override fun toString(): String {
        return "$x:$y"
    }

    private fun toFloat(): Float {
        return x.toFloat() / y
    }

    override fun hashCode(): Int {
        // assuming most sizes are <2^16, doing a rotate will give us perfect hashing
        return y xor (x shl Integer.SIZE / 2 or (x ushr Integer.SIZE / 2))
    }

    override fun compareTo(other: AspectRatio): Int {
        if (equals(other)) {
            return 0
        } else if (toFloat() - other.toFloat() > 0) {
            return 1
        }
        return -1
    }

    /**
     * @return The inverse of this [AspectRatio].
     */
    @Suppress("UNUSED")
    fun inverse(): AspectRatio {
        return of(y, x)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(x)
        dest.writeInt(y)
    }

    companion object {
        private val sCache =
            SparseArrayCompat<SparseArrayCompat<AspectRatio>>(16)

        /**
         * Returns an instance of [AspectRatio] specified by `x` and `y` values.
         * The values `x` and `` will be reduced by their greatest common divider.
         *
         * @param x The width
         * @param y The height
         * @return An instance of [AspectRatio]
         */
        fun of(x: Int, y: Int): AspectRatio {
            @Suppress("NAME_SHADOWING")
            var x = x
            @Suppress("NAME_SHADOWING")
            var y = y
            val gcd = gcd(x, y)
            x /= gcd
            y /= gcd
            var arrayX = sCache[x]
            return if (arrayX == null) {
                val ratio = AspectRatio(x, y)
                arrayX = SparseArrayCompat()
                arrayX.put(y, ratio)
                sCache.put(x, arrayX)
                ratio
            } else {
                var ratio = arrayX[y]
                if (ratio == null) {
                    ratio = AspectRatio(x, y)
                    arrayX.put(y, ratio)
                }
                ratio
            }
        }

        /**
         * Parse an [AspectRatio] from a [String] formatted like "4:3".
         *
         * @param s The string representation of the aspect ratio
         * @return The aspect ratio
         * @throws IllegalArgumentException when the format is incorrect.
         */
        @Suppress("UNUSED")
        fun parse(s: String): AspectRatio {
            val position = s.indexOf(':')
            require(position != -1) { "Malformed aspect ratio: $s" }
            return try {
                val x = s.substring(0, position).toInt()
                val y = s.substring(position + 1).toInt()
                of(x, y)
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Malformed aspect ratio: $s", e)
            }
        }

        private fun gcd(a: Int, b: Int): Int {
            @Suppress("NAME_SHADOWING")
            var a = a
            @Suppress("NAME_SHADOWING")
            var b = b
            while (b != 0) {
                val c = b
                b = a % b
                a = c
            }
            return a
        }

        @Suppress("UNUSED")
        val CREATOR: Parcelable.Creator<AspectRatio?> = object : Parcelable.Creator<AspectRatio?> {
            override fun createFromParcel(source: Parcel): AspectRatio? {
                val x = source.readInt()
                val y = source.readInt()
                return of(x, y)
            }

            override fun newArray(size: Int): Array<AspectRatio?> {
                return arrayOfNulls(size)
            }
        }
    }

}