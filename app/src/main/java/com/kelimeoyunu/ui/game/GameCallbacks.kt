package com.kelimeoyunu.ui.game

/** [GameContent]'in kullanıcı eylemleri; ViewModel'e bağlanır, testte sahte verilir. */
data class GameCallbacks(
    val onLetter: (Int) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onShuffle: () -> Unit = {},
    val onHintLetter: () -> Unit = {},
    val onHintWord: () -> Unit = {},
    val onNextLevel: () -> Unit = {},
) {
    companion object {
        fun of(vm: GameViewModel) = GameCallbacks(
            vm::onLetter, vm::onSubmit, vm::onDelete, vm::onClear,
            vm::onShuffle, vm::onHintLetter, vm::onHintWord, vm::onNextLevel,
        )
    }
}
