package com.kelimeoyunu.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import org.junit.Assert.assertTrue

/** Genel tarama: tıklanabilir her düğümün contentDescription'ı ya da metni dolu olmalı. */
fun assertNoUnlabeledClickables(rule: ComposeContentTestRule) {
    val nodes = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
    assertTrue("tıklanabilir düğüm bulunamadı", nodes.isNotEmpty())
    nodes.forEach { n ->
        val cd = n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString("")
        val text = n.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString("") { it.text }
        assertTrue("etiketsiz tıklanabilir düğüm: ${n.config}", cd.isNotBlank() || text.isNotBlank())
    }
}
