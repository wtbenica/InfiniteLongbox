package dev.benica.infinite_longbox.database.daos

import java.text.Normalizer

/** Builds GLOB patterns that match a typed name whatever its case or accents. */
object AccentFolding {

    // Letters that don't decompose into a base letter plus a mark
    private val EXTRA_BASES = mapOf('ø' to 'o', 'Ø' to 'O', 'đ' to 'd', 'Đ' to 'D', 'ł' to 'l', 'Ł' to 'L', 'ħ' to 'h', 'Ħ' to 'H')

    private fun baseOf(c: Char): Char {
        EXTRA_BASES[c]?.let { return it }
        if (c.code < 0xC0) return c
        val d = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
        return if (d.length > 1 && d[0] in 'A'..'Z' || d.length > 1 && d[0] in 'a'..'z') d[0] else c
    }

    /** Each lowercase base letter with every accented and uppercase form of it. */
    private val forms: Map<Char, String> by lazy {
        val byBase = HashMap<Char, StringBuilder>()
        for (code in 0x41..0x24F) addForm(byBase, code.toChar())
        for (code in 0x1E00..0x1EFF) addForm(byBase, code.toChar())
        for (c in EXTRA_BASES.keys) addForm(byBase, c)
        byBase.mapValues { it.value.toString() }
    }

    private fun addForm(byBase: HashMap<Char, StringBuilder>, c: Char) {
        val base = baseOf(c)
        if (base !in 'A'..'Z' && base !in 'a'..'z') return
        byBase.getOrPut(base.lowercaseChar()) { StringBuilder() }.append(c)
    }

    /** A GLOB pattern for names containing [text] (spaces match anything), ignoring case and accents. */
    fun globContaining(text: String): String = "*${fold(text, spaceMatchesAny = true)}*"

    /** [text] as a GLOB pattern ignoring case and accents, spaces literal, for the caller to put wildcards around. */
    fun globLiteral(text: String): String = fold(text, spaceMatchesAny = false)

    private fun fold(text: String, spaceMatchesAny: Boolean): String = buildString {
        for (c in text) {
            val set = forms[baseOf(c).lowercaseChar()]
            when {
                c == ' ' -> append(if (spaceMatchesAny) "*" else " ")
                set != null -> append('[').append(set).append(']')
                c == '*' || c == '?' || c == '[' -> append('[').append(c).append(']')
                else -> append(c)
            }
        }
    }
}
