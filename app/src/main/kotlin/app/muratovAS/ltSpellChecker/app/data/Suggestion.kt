package app.muratovAS.ltSpellChecker.app.data

data class Suggestion(
    var text: Array<String> = emptyArray(),
    var position: Int = 0,
    var length: Int = 0,
    var isGrammarSuggestion: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Suggestion

        if (!text.contentEquals(other.text)) return false
        if (position != other.position) return false
        if (length != other.length) return false
        if (isGrammarSuggestion != other.isGrammarSuggestion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.contentHashCode()
        result = 31 * result + position
        result = 31 * result + length
        result = 31 * result + isGrammarSuggestion.hashCode()
        return result
    }
}