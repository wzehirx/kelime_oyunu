package com.kelimeoyunu.domain

data class Place(
    val id: String,
    val ad: String,
    val kita: String,
    val bolge: String,
    val baskent: String,
    val bilgi1: String,
    val bilgi2: String,
)

/** [levelInPlace] 1 tabanlıdır; [placeIndex] 0 tabanlıdır. */
data class PlacePosition(val place: Place, val placeIndex: Int, val levelInPlace: Int, val placeLevelCount: Int)

sealed interface PlaceState {
    data object Locked : PlaceState
    data class InProgress(val done: Int, val total: Int) : PlaceState
    data class Completed(val total: Int) : PlaceState
}

/**
 * Seviyeleri ülkelere böler; tüm bilgi 0 tabanlı seviye sırasından türetilir (kayıt alanı gerekmez).
 *
 * Seviye sayısı kuralı: ilk 10 ülke 20'şer, sonraki 30 ülke 50'şer, kalan ülkeler artan seviyeleri
 * eşit paylaşır (artan, ilk ülkelerden başlayarak birer birer dağıtılır). Bu sabit düzen tutmuyorsa
 * (az ülke, az seviye ya da toplam eşleşmiyor) basit bir yedek kural kullanılır: toplam seviye tüm
 * ülkelere eşit bölünür, artan ilk ülkelere birer birer. Seviyeden az ülke varsa yalnızca ilk
 * `totalLevels` ülke kullanılır; böylece her ülkede en az 1 seviye olur ve toplam her zaman [totalLevels]'tir.
 */
class WorldTour(places: List<Place>, totalLevels: Int) {
    private val used: List<Place> = places.take(maxOf(totalLevels, 0))
    private val starts: IntArray // starts[i] = i. ülkenin ilk seviyesinin 0 tabanlı sırası; son eleman toplam

    val placeCount: Int get() = used.size
    val totalLevels: Int get() = starts.last()

    init {
        val counts = levelCounts(used.size, totalLevels)
        starts = IntArray(counts.size + 1)
        for (i in counts.indices) starts[i + 1] = starts[i] + counts[i]
    }

    fun place(placeIndex: Int): Place? = used.getOrNull(placeIndex)

    fun levelCountOf(placeIndex: Int): Int =
        if (placeIndex in used.indices) starts[placeIndex + 1] - starts[placeIndex] else 0

    fun startIndexOf(placeIndex: Int): Int = starts[placeIndex]

    fun positionOf(levelIndex: Int): PlacePosition? {
        if (levelIndex < 0 || levelIndex >= totalLevels) return null
        var lo = 0
        var hi = used.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (starts[mid] <= levelIndex) lo = mid else hi = mid - 1
        }
        return PlacePosition(used[lo], lo, levelIndex - starts[lo] + 1, levelCountOf(lo))
    }

    /** [currentLevelIndex]: oyuncunun şu an oynadığı (henüz bitirmediği) 0 tabanlı seviye. */
    fun placeState(placeIndex: Int, currentLevelIndex: Int): PlaceState {
        if (placeIndex !in used.indices) return PlaceState.Locked
        val total = levelCountOf(placeIndex)
        val start = starts[placeIndex]
        return when {
            currentLevelIndex < start -> PlaceState.Locked
            currentLevelIndex >= start + total -> PlaceState.Completed(total)
            else -> PlaceState.InProgress(currentLevelIndex - start, total)
        }
    }

    fun completedPlaces(currentLevelIndex: Int): Int =
        (0 until placeCount).count { placeState(it, currentLevelIndex) is PlaceState.Completed }

    companion object {
        private const val EARLY_COUNT = 10
        private const val EARLY_LEVELS = 20
        private const val MID_COUNT = 30
        private const val MID_LEVELS = 50

        internal fun levelCounts(placeCount: Int, totalLevels: Int): IntArray {
            if (placeCount <= 0 || totalLevels <= 0) return IntArray(0)
            val early = minOf(placeCount, EARLY_COUNT)
            val mid = minOf(placeCount - early, MID_COUNT)
            val rest = placeCount - early - mid
            val remaining = totalLevels - early * EARLY_LEVELS - mid * MID_LEVELS
            val fixedFits = if (rest > 0) remaining >= rest else remaining == 0
            if (!fixedFits) return share(totalLevels, placeCount)
            val tail = share(remaining, rest)
            return IntArray(placeCount) { i ->
                when {
                    i < early -> EARLY_LEVELS
                    i < early + mid -> MID_LEVELS
                    else -> tail[i - early - mid]
                }
            }
        }

        private fun share(total: Int, parts: Int): IntArray {
            if (parts <= 0) return IntArray(0)
            val base = total / parts
            val extra = total % parts
            return IntArray(parts) { if (it < extra) base + 1 else base }
        }
    }
}
