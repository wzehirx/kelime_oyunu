package com.kelimeoyunu.data

import com.kelimeoyunu.TestAssets
import com.kelimeoyunu.domain.WorldTour
import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** org.json Android'e özgü olduğundan Robolectric ile çalışır. */
@RunWith(RobolectricTestRunner::class)
class PlacesLoaderTest {
    private fun load(text: String) = loadPlaces { ByteArrayInputStream(text.toByteArray()) }
    private val ok = """{"id":"tr","ad":"Türkiye","kita":"Asya","bolge":"ortadogu","baskent":"Ankara","bilgi1":"a","bilgi2":"b"}"""

    @Test fun realAssetHasHundredValidPlacesStartingWithTurkey() {
        val places = loadPlaces { TestAssets.open("places.json") }
        assertEquals(100, places.size)
        assertEquals("Türkiye", places[0].ad)
        assertEquals("Ankara", places[0].baskent)
        assertEquals(100, places.map { it.id }.toSet().size)
        assertEquals(setOf("avrupa", "asya", "ortadogu", "afrika", "amerika", "okyanusya"), places.map { it.bolge }.toSet())
        assertTrue(places.all { it.bilgi1.isNotBlank() && it.bilgi2.isNotBlank() })
    }

    @Test fun realAssetsMakeTheFullTour() {
        val tour = WorldTour(loadPlaces { TestAssets.open("places.json") }, 10_000)
        assertEquals(100, tour.placeCount)
        assertEquals(10_000, tour.totalLevels)
        assertEquals("Türkiye", tour.positionOf(0)!!.place.ad)
        assertEquals(20, tour.positionOf(0)!!.placeLevelCount)
        assertEquals(tour.place(99), tour.positionOf(9_999)!!.place)
    }

    @Test fun validSingleEntry() = assertEquals(listOf("Türkiye"), load("[$ok]").map { it.ad })

    @Test fun brokenInputsGiveEmptyListWithoutCrashing() {
        listOf(
            "", "   ", "not json", "{}", "[1,2]", "[$ok", "[$ok,{}]",
            """[{"id":"x","ad":"A"}]""",
            """[$ok,{"id":"x","ad":"A","kita":"K","bolge":"mars","baskent":"B","bilgi1":"a","bilgi2":"b"}]""",
            """[{"id":"x","ad":" ","kita":"K","bolge":"asya","baskent":"B","bilgi1":"a","bilgi2":"b"}]""",
        ).forEach { assertEquals("'$it'", emptyList<Any>(), load(it)) }
    }

    @Test fun missingFileGivesEmptyList() {
        assertEquals(emptyList<Any>(), loadPlaces { throw java.io.FileNotFoundException("yok") })
        assertEquals(emptyList<Any>(), loadPlaces { throw IOException("bozuk") })
    }
}
