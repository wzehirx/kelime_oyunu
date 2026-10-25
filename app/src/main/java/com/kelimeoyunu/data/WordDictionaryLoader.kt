package com.kelimeoyunu.data

import com.kelimeoyunu.domain.WordDictionary
import java.io.InputStream

/** Satır başına bir kelime içeren UTF-8 akışından sözlük kurar. Akış açılamazsa IOException atar. */
fun loadWords(open: () -> InputStream): WordDictionary =
    open().bufferedReader(Charsets.UTF_8).useLines { WordDictionary.fromLines(it) }
