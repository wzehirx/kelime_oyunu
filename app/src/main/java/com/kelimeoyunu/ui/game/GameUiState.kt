package com.kelimeoyunu.ui.game

import com.kelimeoyunu.domain.Place
import com.kelimeoyunu.domain.PlacePosition
import com.kelimeoyunu.domain.SubmitResult

/** [index] harf havuzundaki kimliktir (karıştırınca değişmez); [tiles] listesi gösterim sırasındadır. */
data class LetterTile(val index: Int, val char: Char, val used: Boolean)

/** [cells]: açık harf ya da gizliyse null. */
data class WordRow(val number: Int, val length: Int, val cells: List<Char?>, val found: Boolean)

/**
 * Çapraz bulmaca ızgarasının görsel verisi (ekran okuyucuya sunulmaz; bilgi kelime listesindedir).
 * [letter] yalnızca açık kutularda doludur (bulunmuş ya da ipucuyla açılmış); [hinted] ipucuyla açılmış ama
 * henüz bir bulunmuş kelimenin parçası olmayan kutudur. Koordinatlar ızgara sınırlarına göre 0 tabanlıdır.
 */
data class GridCellUi(val row: Int, val col: Int, val letter: Char?, val hinted: Boolean)

data class GridUi(val rows: Int, val cols: Int, val cells: List<GridCellUi>)

sealed interface GameUiState {
    data object Loading : GameUiState
    data class Error(val message: String) : GameUiState
    data object AllDone : GameUiState
    data class Ready(
        val levelNumber: Int,
        val tiles: List<LetterTile>,
        val currentWord: String,
        val coins: Int,
        val remainingWords: Int,
        val bonusMeter: Int,
        val bonusTarget: Int,
        val wordRows: List<WordRow>,
        val levelComplete: Boolean,
        /** Dünya turu konumu; tur kapalıysa null. */
        val place: PlacePosition? = null,
        /** Seçili harflerin [LetterTile.index] kimlikleri, seçim sırasıyla (jeton sıra rozeti için). */
        val selection: List<Int> = emptyList(),
        /** Görsel ızgara; verilmemişse (testler) çizilmez. */
        val grid: GridUi? = null,
    ) : GameUiState
}

sealed interface GameEvent {
    data class LetterAdded(val char: Char, val index: Int, val position: Int) : GameEvent
    data object Deleted : GameEvent
    data object Cleared : GameEvent
    data class Submitted(val result: SubmitResult) : GameEvent
    data object Shuffled : GameEvent
    /** [coinsLeft]: ipucu bedeli düşüldükten sonraki bakiye (sonradan gelen ödüller dahil değil). */
    data class HintUsed(val coinsLeft: Int) : GameEvent
    data object NotEnoughCoins : GameEvent
    data object NoHintAvailable : GameEvent
    /** [silent]: ses efekti çalınmaz (ülke bitişinde tek jingle çalar); duyuru etkilenmez. */
    data class CoinsEarned(val amount: Int, val silent: Boolean = false) : GameEvent
    data object BonusMeterFull : GameEvent
    /** [placeCompleted]: bu seviye ülkenin sonuncusu; hemen ardından [PlaceCompleted] gelir (ses: yalnızca onun jingle'ı). */
    data class LevelCompleted(val placeCompleted: Boolean = false) : GameEvent
    /** Bir ülkenin ilk seviyesi açıldı (kalıcı olarak ülke başına bir kez). */
    data class PlaceEntered(val place: Place, val placeLevelCount: Int) : GameEvent
    /** Ülkenin son seviyesi bitti; [LevelCompleted]'dan hemen sonra gelir. [bonusCoins] zaten bakiyeye eklenmiştir. */
    data class PlaceCompleted(val place: Place, val bonusCoins: Int, val nextPlaceName: String?) : GameEvent
    data object AllLevelsDone : GameEvent
}
