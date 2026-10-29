package com.creazzy.novakeys.ime

/** Offline Unicode typography helpers. These are Unicode characters, not installed fonts. */
object UnicodeTextStyles {
    enum class Style(val title: String) {
        BOLD("Bold"), ITALIC("Italic"), BOLD_ITALIC("Bold Italic"),
        SCRIPT("Script"), FRAKTUR("Fraktur"), DOUBLE_STRUCK("Double"),
        SANS("Sans"), MONOSPACE("Mono"), FULLWIDTH("Fullwidth"),
        CIRCLED("Circled"), SMALL_CAPS("Small caps"), STRIKETHROUGH("Strike"), UNDERLINE("Underline"), OVERLINE("Overline"), UPSIDE_DOWN("Upside down"), LEET("1337"), GLITCH("Glitch")
    }

    private val smallCaps = mapOf(
        'a' to "ᴀ", 'b' to "ʙ", 'c' to "ᴄ", 'd' to "ᴅ", 'e' to "ᴇ",
        'f' to "ғ", 'g' to "ɢ", 'h' to "ʜ", 'i' to "ɪ", 'j' to "ᴊ",
        'k' to "ᴋ", 'l' to "ʟ", 'm' to "ᴍ", 'n' to "ɴ", 'o' to "ᴏ",
        'p' to "ᴘ", 'r' to "ʀ", 't' to "ᴛ", 'u' to "ᴜ", 'v' to "ᴠ",
        'w' to "ᴡ", 'y' to "ʏ", 'z' to "ᴢ"
    )
    private val leet = mapOf('a' to "4", 'e' to "3", 'i' to "1", 'o' to "0", 's' to "5", 't' to "7", 'g' to "9", 'b' to "8")

    fun transform(text: String, style: Style): String = when (style) {
        Style.SMALL_CAPS -> text.map { smallCaps[it.lowercaseChar()] ?: it.toString() }.joinToString("")
        Style.LEET -> text.map { leet[it.lowercaseChar()] ?: it.toString() }.joinToString("")
        Style.STRIKETHROUGH -> combineMark(text, "\u0336")
        Style.UNDERLINE -> combineMark(text, "\u0332")
        Style.OVERLINE -> combineMark(text, "\u0305")
        Style.UPSIDE_DOWN -> upsideDown(text)
        Style.GLITCH -> zalgo(text)
        Style.FULLWIDTH -> text.map { ch ->
            when {
                ch == ' ' -> '\u3000'
                ch in '!'..'~' -> (ch.code + 0xFEE0).toChar()
                else -> ch
            }
        }.joinToString("")
        Style.CIRCLED -> text.map { ch ->
            when (ch) {
                in 'A'..'Z' -> String(Character.toChars(0x24B6 + ch.code - 'A'.code))
                in 'a'..'z' -> String(Character.toChars(0x24D0 + ch.code - 'a'.code))
                in '1'..'9' -> String(Character.toChars(0x2460 + ch.code - '1'.code))
                '0' -> "⓪"
                else -> ch.toString()
            }
        }.joinToString("")
        else -> mapLatin(text, style)
    }

    private fun mapLatin(text: String, style: Style): String {
        val upperBase: Int
        val lowerBase: Int
        when (style) {
            Style.BOLD -> { upperBase = 0x1D400; lowerBase = 0x1D41A }
            Style.ITALIC -> { upperBase = 0x1D434; lowerBase = 0x1D44E }
            Style.BOLD_ITALIC -> { upperBase = 0x1D468; lowerBase = 0x1D482 }
            Style.SCRIPT -> { upperBase = 0x1D49C; lowerBase = 0x1D4B6 }
            Style.FRAKTUR -> { upperBase = 0x1D504; lowerBase = 0x1D51E }
            Style.DOUBLE_STRUCK -> { upperBase = 0x1D538; lowerBase = 0x1D552 }
            Style.SANS -> { upperBase = 0x1D5A0; lowerBase = 0x1D5BA }
            Style.MONOSPACE -> { upperBase = 0x1D670; lowerBase = 0x1D68A }
            else -> return text
        }
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val mapped = when (cp) {
                in 65..90 -> specialOrCodepoint(style, cp, upperBase + cp - 65)
                in 97..122 -> specialOrCodepoint(style, cp, lowerBase + cp - 97)
                else -> String(Character.toChars(cp))
            }
            out.append(mapped)
            i += Character.charCount(cp)
        }
        return out.toString()
    }

    private fun specialOrCodepoint(style: Style, cp: Int, mapped: Int): String {
        val special = when (style) {
            Style.SCRIPT -> mapOf('B'.code to 0x212C, 'E'.code to 0x2130, 'F'.code to 0x2131, 'H'.code to 0x210B, 'I'.code to 0x2110, 'L'.code to 0x2112, 'M'.code to 0x2133, 'R'.code to 0x211B)
            Style.FRAKTUR -> mapOf('C'.code to 0x212D, 'H'.code to 0x210C, 'I'.code to 0x2111, 'R'.code to 0x211C, 'Z'.code to 0x2128)
            Style.DOUBLE_STRUCK -> mapOf('C'.code to 0x2102, 'H'.code to 0x210D, 'N'.code to 0x2115, 'P'.code to 0x2119, 'Q'.code to 0x211A, 'R'.code to 0x211D, 'Z'.code to 0x2124)
            else -> emptyMap()
        }
        return String(Character.toChars(special[cp] ?: mapped))
    }

    private fun combineMark(text: String, mark: String): String =
        text.map { if (it.isWhitespace()) it.toString() else it + mark }.joinToString("")

    private fun upsideDown(text: String): String {
        val replacements = mapOf(
            'a' to "ɐ", 'b' to "q", 'c' to "ɔ", 'd' to "p", 'e' to "ǝ",
            'f' to "ɟ", 'g' to "ƃ", 'h' to "ɥ", 'i' to "ᴉ", 'j' to "ɾ",
            'k' to "ʞ", 'l' to "ן", 'm' to "ɯ", 'n' to "u", 'o' to "o",
            'p' to "d", 'q' to "b", 'r' to "ɹ", 's' to "s", 't' to "ʇ",
            'u' to "n", 'v' to "ʌ", 'w' to "ʍ", 'x' to "x", 'y' to "ʎ",
            'A' to "∀", 'B' to "𐐒", 'C' to "Ɔ", 'E' to "Ǝ", 'F' to "Ⅎ",
            'G' to "פ", 'H' to "H", 'I' to "I", 'J' to "ſ", 'L' to "˥",
            'M' to "W", 'N' to "N", 'P' to "Ԁ", 'R' to "ᴚ", 'T' to "⊥",
            'U' to "∩", 'V' to "Λ", 'Y' to "⅄", '.' to "˙", ',' to "'",
            '?' to "¿", '!' to "¡", '(' to ")", ')' to "(", '[' to "]", ']' to "["
        )
        return text.reversed().map { replacements[it] ?: it.toString() }.joinToString("")
    }

    fun zalgo(text: String): String {
        val marks = listOf("\u030d", "\u0336", "\u0312", "\u033f", "\u0358", "\u0327", "\u0315")
        return buildString {
            text.forEachIndexed { index, ch ->
                append(ch)
                if (!ch.isWhitespace() && ch.isLetterOrDigit()) {
                    append(marks[index % marks.size])
                    if (index % 2 == 0) append("\u0301")
                }
            }
        }
    }

    fun decorate(text: String, decoration: String): String = when (decoration) {
        "frame" -> "꧁ $text ꧂"
        "gothic" -> "༒ $text ༒"
        "stars" -> "✦ ✧ $text ✧ ✦"
        "heart" -> "♡ $text ♥"
        "brackets" -> "『 $text 』"
        "terminal" -> "[ root@novakeys:~# $text ]"
        "arrows" -> "➶ $text ⇢"
        "runes" -> "ᚺᚨ $text ᚾᛟ"
        else -> text
    }
}
