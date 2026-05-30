package com.vynce.music.utils.cipher

import org.junit.Test

/**
 * Cleaned up test showing only the successful regex patterns for YouTube deobfuscation.
 */
class RegexTest {

    @Test
    fun testSuccessfulSignaturePatterns() {
        val samples = listOf(
            "a&&(b=JI(48,decodeURIComponent(b)))",
            "&& (c=hJ(decodeURIComponent(c)))",
            "m=f1(decodeURIComponent(h.s))",
            "p.sig||VX(p.s)",
            "a.signature||kL(a.s)",
            "bc=function(a){a=a.split(\"\");dc.XF(a,24);return a.join(\"\")}",
            "var d=a.split(\"\");f1.sR(d,1);return d.join(\"\")"
        )

        val patterns = listOf(
            // 1. &&(VAR=FUNC(NUM,decodeURIComponent(VAR))
            Regex("""(?:\b|&&)\s*\([\w$]+?=([\w$]{2,})\(\s*(?:\d+,\s*)?decodeURIComponent\("""),
            // 2. m=FUNC(decodeURIComponent(h.s))
            Regex("""\bm=([\w$]{2,})\(decodeURIComponent\(h\.s\)\)"""),
            // 3. .sig||FUNC( or .signature||FUNC(
            Regex("""\.(?:sig|signature)\|\|([\w$]{2,})\("""),
            // 4. name=function(a){a=a.split("")
            Regex("""(?:\b|[^a-zA-Z0-9$])([\w$]{2,})\s*=\s*function\(\s*a\s*\)\s*\{\s*a\s*=\s*a\.split\(\s*["']{2}\s*\)"""),
            // 5. a=a.split(""); OBJ.method(a, ...)
            Regex("""a=a\.split\(\s*["']{2}\s*\);\s*([\w$]{2,})\.""")
        )

        println("=== Successful Signature Patterns ===")
        for (sample in samples) {
            var matched = false
            for ((idx, pattern) in patterns.withIndex()) {
                val match = pattern.find(sample)
                if (match != null) {
                    println("Match Found! Pattern $idx extracted: ${match.groupValues[1]} from sample: $sample")
                    matched = true
                    break
                }
            }
            if (!matched) println("FAIL: No match for sample: $sample")
        }
    }

    @Test
    fun testSuccessfulNTransformPatterns() {
        val samples = listOf(
            """.get("n"))&&(b=a[0](b))""",
            """.get("n"))&&(b=Vea(b))""",
            """a.get("n")&&(c=d[15](c))"""
        )

        val patterns = listOf(
            // 1. .get("n"))&&(b=FUNC[IDX](VAR)
            Regex("""\.get\("n"\)\)&&\(b=([\w$]+)(?:\[(\d+)\])?\([\w$]\)"""),
            // 2. .get("n")&&(b=FUNC(VAR)
            Regex("""\.get\("n"\)\s*&&\s*\([\w$]+?=([\w$]+)(?:\[(\d+)\])?\([\w$]+\)""")
        )

        println("=== Successful N-Transform Patterns ===")
        for (sample in samples) {
            for ((idx, pattern) in patterns.withIndex()) {
                val match = pattern.find(sample)
                if (match != null) {
                    val name = match.groupValues[1]
                    val index = if (match.groupValues.size > 2) "[${match.groupValues[2]}]" else ""
                    println("Match Found! Pattern $idx extracted: $name$index from sample: $sample")
                    break
                }
            }
        }
    }

    @Test
    fun testSuccessfulUrlPatterns() {
        val contents = listOf(
            """{"jsUrl":"/s/player/f4c47414/player_ias.vflset/en_US/base.js"}""",
            """var j="/s/player/74edf1a3/player_ias.vflset/en_US/base.js";""",
            """<script src="//www.youtube.com/s/player/9fc68080/player_ias.vflset/en_US/base.js"></script>""",
            """https://www.youtube.com/s/player/57f5d44f/player_ias.vflset/en_US/base.js"""
        )

        val urlPattern = Regex("""(?:"|')((?:(?:https?:)?//(?:www\.)?youtube\.com)?/s/player/[a-z0-9]{8}/[^"']+base\.js)(?:"|')""")

        println("=== Successful URL Patterns ===")
        for (content in contents) {
            val match = urlPattern.find(content)
            if (match != null) {
                println("Match Found! Extracted URL: ${match.groupValues[1]}")
            }
        }
    }
}











