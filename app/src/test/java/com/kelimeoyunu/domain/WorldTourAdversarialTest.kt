package com.kelimeoyunu.domain

import com.kelimeoyunu.TestPlaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** QA: WorldTour dağıtım kuralı, sınır durumları ve 10.000 seviyenin tamamı için tutarlılık. */
class WorldTourAdversarialTest {
    private val tour = WorldTour(TestPlaces.list(100), 10_000)

    @Test fun everyOneOfTenThousandLevelsMapsConsistentlyAndMonotonically() {
        var prevPlace = 0
        var prevInPlace = 0
        for (i in 0 until 10_000) {
            val p = tour.positionOf(i)!!
            assertTrue("level $i levelInPlace ${p.levelInPlace}/${p.placeLevelCount}", p.levelInPlace in 1..p.placeLevelCount)
            assertEquals(tour.levelCountOf(p.placeIndex), p.placeLevelCount)
            assertEquals(i, tour.startIndexOf(p.placeIndex) + p.levelInPlace - 1)
            assertEquals(tour.place(p.placeIndex), p.place)
            if (i == 0) {
                assertEquals(0, p.placeIndex); assertEquals(1, p.levelInPlace)
            } else if (p.placeIndex == prevPlace) {
                assertEquals("level $i", prevInPlace + 1, p.levelInPlace)
            } else {
                assertEquals("country jump at $i", prevPlace + 1, p.placeIndex)
                assertEquals(1, p.levelInPlace)
                assertEquals("previous country must end exactly at boundary", tour.levelCountOf(prevPlace), prevInPlace)
            }
            prevPlace = p.placeIndex; prevInPlace = p.levelInPlace
        }
        assertEquals(99, prevPlace)
        assertEquals(tour.levelCountOf(99), prevInPlace)
    }

    @Test fun outOfRangeLevelsHaveNoPosition() {
        assertNull(tour.positionOf(-1)); assertNull(tour.positionOf(10_000)); assertNull(tour.positionOf(Int.MAX_VALUE))
        assertNull(tour.positionOf(Int.MIN_VALUE))
        assertEquals(0, tour.levelCountOf(-1)); assertEquals(0, tour.levelCountOf(100))
        assertNull(tour.place(100)); assertNull(tour.place(-1))
    }

    @Test fun ruleCountsMatchSpecAndSumToTenThousand() {
        val counts = (0 until 100).map { tour.levelCountOf(it) }
        assertTrue(counts.take(10).all { it == 20 })
        assertTrue(counts.subList(10, 40).all { it == 50 })
        val rest = counts.subList(40, 100)
        assertEquals(60, rest.size)
        assertEquals(8300, rest.sum())
        assertTrue("kalan ülkeler 138 ya da 139", rest.all { it == 138 || it == 139 })
        assertEquals(10_000, counts.sum())
        assertEquals(10_000, tour.totalLevels)
        assertTrue(counts.all { it >= 1 })
    }

    @Test fun firstHundredLevelsSpanFiveCountries() {
        assertEquals(5, (0 until 100).map { tour.positionOf(it)!!.placeIndex }.toSet().size)
        assertEquals(4, tour.positionOf(99)!!.placeIndex)
        assertEquals(5, tour.positionOf(100)!!.placeIndex)
    }

    @Test fun smallPlaceListsUseFallbackRuleAndStillSumExactly() {
        for ((n, total) in listOf(8 to 10_000, 40 to 10_000, 1 to 10_000, 10 to 10_000, 11 to 10_000, 99 to 10_000, 101 to 10_000, 100 to 9_999, 100 to 100, 100 to 99, 100 to 1, 3 to 1, 100 to 1700, 100 to 1760, 50 to 1700)) {
            val t = WorldTour(TestPlaces.list(n), total)
            assertEquals("n=$n total=$total", total, t.totalLevels)
            assertEquals(minOf(n, total), t.placeCount)
            val counts = (0 until t.placeCount).map { t.levelCountOf(it) }
            assertEquals("n=$n total=$total", total, counts.sum())
            assertTrue("n=$n total=$total sıfır seviyeli ülke $counts", counts.all { it >= 1 })
            assertTrue("n=$n total=$total", counts.max() - counts.min() <= 1 || n >= 10 && counts.size > 10)
            var covered = 0
            for (i in 0 until total.coerceAtMost(3000)) { t.positionOf(i)!!; covered++ }
            assertEquals(total.coerceAtMost(3000), covered)
            assertNull(t.positionOf(total))
        }
    }

    @Test fun eightAndFortyPlaceFallbackIsEven() {
        assertEquals(List(8) { 1250 }, (0 until 8).map { WorldTour(TestPlaces.list(8), 10_000).levelCountOf(it) })
        assertEquals(List(40) { 250 }, (0 until 40).map { WorldTour(TestPlaces.list(40), 10_000).levelCountOf(it) })
    }

    @Test fun degenerateInputsDoNotCrash() {
        val empty = WorldTour(emptyList(), 100)
        assertEquals(0, empty.placeCount); assertNull(empty.positionOf(0)); assertEquals(0, empty.totalLevels)
        val zero = WorldTour(TestPlaces.list(5), 0)
        assertEquals(0, zero.placeCount); assertNull(zero.positionOf(0))
        val neg = WorldTour(TestPlaces.list(5), -3)
        assertEquals(0, neg.placeCount); assertNull(neg.positionOf(0))
        assertEquals(0, neg.completedPlaces(0))
    }

    @Test fun placeStateIsExhaustiveAndConsistentWithCompletedCount() {
        for (cur in listOf(0, 19, 20, 199, 200, 249, 250, 1699, 1700, 1838, 1839, 9999, 10_000, 12_345)) {
            var completed = 0; var inProgress = 0
            for (p in 0 until 100) when (val s = tour.placeState(p, cur)) {
                PlaceState.Locked -> assertTrue(cur < tour.startIndexOf(p))
                is PlaceState.InProgress -> { inProgress++; assertTrue(s.done in 0 until s.total) }
                is PlaceState.Completed -> { completed++; assertEquals(tour.levelCountOf(p), s.total) }
            }
            assertEquals("cur=$cur", completed, tour.completedPlaces(cur))
            assertEquals("cur=$cur", if (cur >= 10_000) 0 else 1, inProgress)
        }
        assertEquals(PlaceState.Locked, tour.placeState(100, 5))
        assertEquals(PlaceState.Locked, tour.placeState(-1, 5))
    }
}
