package com.kelimeoyunu.domain

import com.kelimeoyunu.TestPlaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldTourTest {
    private val tour = WorldTour(TestPlaces.list(100), 10_000)

    private fun counts(t: WorldTour) = (0 until t.placeCount).map { t.levelCountOf(it) }

    @Test fun standardRuleSplitsTenThousandLevels() {
        val c = counts(tour)
        assertEquals(100, tour.placeCount)
        assertEquals(10_000, c.sum())
        assertEquals(List(10) { 20 }, c.take(10))
        assertEquals(List(30) { 50 }, c.subList(10, 40))
        // kalan 8300 seviye 60 ülkeye: 138'er, artan 20 seviye ilk 20 ülkeye birer birer
        assertEquals(List(20) { 139 } + List(40) { 138 }, c.drop(40))
        assertTrue(c.all { it >= 1 })
        assertEquals(10_000, tour.totalLevels)
    }

    @Test fun boundariesAreConsistent() {
        var next = 0
        for (i in 0 until tour.placeCount) {
            assertEquals(next, tour.startIndexOf(i))
            next += tour.levelCountOf(i)
            // sınırdaki iki seviye doğru ülkelerde
            assertEquals(i, tour.positionOf(tour.startIndexOf(i))!!.placeIndex)
            assertEquals(1, tour.positionOf(tour.startIndexOf(i))!!.levelInPlace)
            assertEquals(i, tour.positionOf(next - 1)!!.placeIndex)
            assertEquals(tour.levelCountOf(i), tour.positionOf(next - 1)!!.levelInPlace)
        }
        assertEquals(10_000, next)
    }

    @Test fun positionOfKnownLevels() {
        val first = tour.positionOf(0)!!
        assertEquals("Türkiye", first.place.ad)
        assertEquals(Triple(0, 1, 20), Triple(first.placeIndex, first.levelInPlace, first.placeLevelCount))
        assertEquals(Triple(0, 20, 20), tour.positionOf(19)!!.let { Triple(it.placeIndex, it.levelInPlace, it.placeLevelCount) })
        val second = tour.positionOf(20)!!
        assertEquals(Triple(1, 1, 20), Triple(second.placeIndex, second.levelInPlace, second.placeLevelCount))
        val last = tour.positionOf(9_999)!!
        assertEquals(99, last.placeIndex)
        assertEquals(last.placeLevelCount, last.levelInPlace)
        assertEquals(138, last.placeLevelCount)
        assertNull(tour.positionOf(10_000))
        assertNull(tour.positionOf(-1))
    }

    @Test fun placeStates() {
        assertEquals(PlaceState.InProgress(0, 20), tour.placeState(0, 0))
        assertEquals(PlaceState.Locked, tour.placeState(1, 0))
        assertEquals(PlaceState.InProgress(5, 20), tour.placeState(0, 5))
        assertEquals(PlaceState.Completed(20), tour.placeState(0, 20))
        assertEquals(PlaceState.InProgress(0, 20), tour.placeState(1, 20))
        assertEquals(1, tour.completedPlaces(20))
        assertEquals(100, tour.completedPlaces(10_000))
        assertEquals(PlaceState.Locked, tour.placeState(500, 0))
    }

    @Test fun fewerPlacesOrLevelsStillSumExactlyWithAtLeastOneEach() {
        for ((places, total) in listOf(5 to 10_000, 40 to 1_700, 40 to 1_701, 45 to 1_705, 100 to 300, 100 to 7, 3 to 3, 1 to 1, 100 to 100)) {
            val t = WorldTour(TestPlaces.list(places), total)
            val c = counts(t)
            assertEquals("$places/$total toplam", total, c.sum())
            assertTrue("$places/$total en az 1", c.all { it >= 1 })
            assertEquals(minOf(places, total), t.placeCount)
            assertEquals(total - 1, t.positionOf(total - 1)!!.let { t.startIndexOf(it.placeIndex) + it.levelInPlace - 1 })
        }
        // yedek kural: eşit paylaşım, artan ilk ülkelere
        assertEquals(listOf(3, 3, 2, 2), counts(WorldTour(TestPlaces.list(4), 10)))
        // sabit düzen tam tutuyorsa korunur
        assertEquals(List(10) { 20 } + List(30) { 50 }, counts(WorldTour(TestPlaces.list(40), 1_700)))
    }

    @Test fun emptyInputsGiveEmptyTour() {
        for (t in listOf(WorldTour(emptyList(), 100), WorldTour(TestPlaces.list(3), 0), WorldTour(TestPlaces.list(3), -5))) {
            assertEquals(0, t.placeCount)
            assertNull(t.positionOf(0))
        }
    }
}
