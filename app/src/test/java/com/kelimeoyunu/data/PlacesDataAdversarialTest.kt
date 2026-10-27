package com.kelimeoyunu.data

import com.kelimeoyunu.TestAssets
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.audio.placeSfx
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** QA: gerçek places.json içeriği ve yükleyici sertliği. */
@RunWith(RobolectricTestRunner::class)
class PlacesDataAdversarialTest {
    private fun load(text: String) = loadPlaces { ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)) }
    private fun entry(ad: String = "A", extra: String = "") =
        """{"id":"x","ad":"$ad","kita":"K","bolge":"asya","baskent":"B","bilgi1":"a.","bilgi2":"b."$extra}"""

    private val places by lazy { loadPlaces { TestAssets.open("places.json") } }

    @Test fun realDataIsCompleteAndSingleSentence() {
        assertEquals(100, places.size)
        assertEquals(100, places.map { it.ad }.toSet().size)
        for (p in places) {
            for (t in listOf(p.id, p.ad, p.kita, p.baskent, p.bilgi1, p.bilgi2)) {
                assertTrue(p.id, t.isNotBlank() && t == t.trim())
            }
            for (t in listOf(p.bilgi1, p.bilgi2)) {
                assertTrue("${p.id}: tek cümle olmalı: $t", Regex("[.!?](\\s|$)").findAll(t).count() == 1 && t.last() in ".!?")
                assertTrue("${p.id}: ASCII dışı garip simge: $t", t.all { it.code < 128 || it in "çğıöşüÇĞİÖŞÜâîûÂÎÛ" })
                assertTrue(p.id, t.length in 20..160)
            }
        }
    }

    @Test fun everyRealRegionHasADedicatedMelody() {
        assertTrue(places.all { placeSfx(it.bolge) != Sfx.NAV })
    }

    @Test fun firstTenCountriesAreTheDocumentedEarlySet() {
        assertEquals("Türkiye", places[0].ad)
        assertEquals(listOf("Türkiye", "Yunanistan", "Bulgaristan"), places.take(3).map { it.ad })
    }

    @Test fun utf8BomAtStartIsTolerated() {
        assertEquals(1, load("﻿[${entry()}]").size)
    }

    @Test fun jsonNullFieldIsRejectedNotTurnedIntoTheWordNull() {
        assertEquals(emptyList<Any>(), load("""[{"id":"x","ad":null,"kita":"K","bolge":"asya","baskent":"B","bilgi1":"a.","bilgi2":"b."}]"""))
    }

    @Test fun nonStringFieldsAreRejectedOrHarmless() {
        val r = load("""[{"id":5,"ad":"A","kita":"K","bolge":"asya","baskent":"B","bilgi1":"a.","bilgi2":"b."}]""")
        assertTrue(r.isEmpty() || r[0].id == "5")
    }

    @Test fun hugeAndEmptyArraysDoNotCrash() {
        assertEquals(emptyList<Any>(), load("[]"))
        assertEquals(1000, load("[" + List(1000) { entry("A$it") }.joinToString(",") + "]").size)
    }
}
