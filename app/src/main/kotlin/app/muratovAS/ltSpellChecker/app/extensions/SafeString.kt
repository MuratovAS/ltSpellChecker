package app.muratovAS.ltSpellChecker.app.extensions

fun Any?.toSafeStringSet(): Set<String> = (this as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()