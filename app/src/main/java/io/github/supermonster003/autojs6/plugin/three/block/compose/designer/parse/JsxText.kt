package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

/**
 * The string values TypeScript's JSX transform gives JSX text children and quoted attribute values, so the
 * designer reads exactly what the compiled script passes to compose (TypeScript 6.0.3 `visitJsxText`,
 * `fixupWhitespaceAndDecodeEntities`, `decodeEntities`), and the conditions under which the TSX writer may
 * spell a string as bare JSX text or as a quoted attribute without changing that value.
 */
internal object JsxText {

    /**
     * The value of a JSX text child, or null when the transform drops it: lines are trimmed (the first line
     * keeps its leading, the last line its trailing white space), blank lines are dropped, the rest are
     * joined with one space, then entities are decoded per line. Returns null as well for an entity whose
     * code point TypeScript cannot encode; [invalidEntity] reports it.
     */
    fun childValue(raw: String, invalidEntity: (String) -> Nothing): String? {
        var accumulated: String? = null
        var firstNonWhitespace = 0
        var lastNonWhitespace = -1
        fun add(line: String) {
            val decoded = decodeEntities(line, invalidEntity)
            accumulated = accumulated?.let { "$it $decoded" } ?: decoded
        }
        for (index in raw.indices) {
            val char = raw[index]
            if (isLineBreak(char)) {
                if (firstNonWhitespace != -1 && lastNonWhitespace != -1) add(raw.substring(firstNonWhitespace, lastNonWhitespace + 1))
                firstNonWhitespace = -1
            } else if (!isWhiteSpaceSingleLine(char)) {
                lastNonWhitespace = index
                if (firstNonWhitespace == -1) firstNonWhitespace = index
            }
        }
        if (firstNonWhitespace != -1) add(raw.substring(firstNonWhitespace))
        return accumulated
    }

    /** The value of a quoted JSX attribute: no escape sequences, entities decoded. */
    fun attributeValue(raw: String, invalidEntity: (String) -> Nothing): String = decodeEntities(raw, invalidEntity)

    private val entityPattern = Regex("&((#((\\d+)|x([\\da-fA-F]+)))|(\\w+));")

    fun decodeEntities(text: String, invalidEntity: (String) -> Nothing): String {
        if ('&' !in text) return text
        return entityPattern.replace(text) { match ->
            val decimal = match.groups[4]?.value
            val hex = match.groups[5]?.value
            val word = match.groups[6]?.value
            val codePoint = when {
                decimal != null -> decimal.toBigInteger()
                hex != null -> hex.toBigInteger(16)
                else -> ENTITIES[word]?.toBigInteger() ?: return@replace match.value
            }
            if (codePoint > MAX_CODE_POINT) invalidEntity(match.value)
            String(Character.toChars(codePoint.toInt()))
        }
    }

    /** True when [value] written as a bare JSX text child reads back as [value]. */
    fun isSafeChild(value: String): Boolean =
        value.isNotEmpty() && !isWhiteSpaceSingleLine(value.first()) && !isWhiteSpaceSingleLine(value.last()) &&
            value.none { isLineBreak(it) || it in "{}<>&" || it.code < 0x20 }

    /** True when [value] written as `name='value'` reads back as [value]. */
    fun isSafeAttribute(value: String): Boolean = value.none { isLineBreak(it) || it in "'&\\" || it.code < 0x20 }

    fun isLineBreak(char: Char): Boolean = char == '\n' || char == '\r' || char == '\u2028' || char == '\u2029'

    fun isWhiteSpaceSingleLine(char: Char): Boolean = char == ' ' || char == '\t' || char == '\u000B' || char == '\u000C' ||
        char == '\u00A0' || char == '\u0085' || char == '\u1680' || char in '\u2000'..'\u200B' || char == '\u202F' ||
        char == '\u205F' || char == '\u3000' || char == '\uFEFF'

    private val MAX_CODE_POINT = Character.MAX_CODE_POINT.toBigInteger()

    /** TypeScript's JSX entity table (`entities` in typescript.js 6.0.3), 253 names. */
    private val ENTITIES: Map<String, Int> = (
        "quot=34 amp=38 apos=39 lt=60 gt=62 nbsp=160 iexcl=161 cent=162 pound=163 curren=164 yen=165 brvbar=166 " +
            "sect=167 uml=168 copy=169 ordf=170 laquo=171 not=172 shy=173 reg=174 macr=175 deg=176 plusmn=177 sup2=178 " +
            "sup3=179 acute=180 micro=181 para=182 middot=183 cedil=184 sup1=185 ordm=186 raquo=187 frac14=188 frac12=189 " +
            "frac34=190 iquest=191 Agrave=192 Aacute=193 Acirc=194 Atilde=195 Auml=196 Aring=197 AElig=198 Ccedil=199 " +
            "Egrave=200 Eacute=201 Ecirc=202 Euml=203 Igrave=204 Iacute=205 Icirc=206 Iuml=207 ETH=208 Ntilde=209 " +
            "Ograve=210 Oacute=211 Ocirc=212 Otilde=213 Ouml=214 times=215 Oslash=216 Ugrave=217 Uacute=218 Ucirc=219 " +
            "Uuml=220 Yacute=221 THORN=222 szlig=223 agrave=224 aacute=225 acirc=226 atilde=227 auml=228 aring=229 " +
            "aelig=230 ccedil=231 egrave=232 eacute=233 ecirc=234 euml=235 igrave=236 iacute=237 icirc=238 iuml=239 " +
            "eth=240 ntilde=241 ograve=242 oacute=243 ocirc=244 otilde=245 ouml=246 divide=247 oslash=248 ugrave=249 " +
            "uacute=250 ucirc=251 uuml=252 yacute=253 thorn=254 yuml=255 OElig=338 oelig=339 Scaron=352 scaron=353 " +
            "Yuml=376 fnof=402 circ=710 tilde=732 Alpha=913 Beta=914 Gamma=915 Delta=916 Epsilon=917 Zeta=918 Eta=919 " +
            "Theta=920 Iota=921 Kappa=922 Lambda=923 Mu=924 Nu=925 Xi=926 Omicron=927 Pi=928 Rho=929 Sigma=931 Tau=932 " +
            "Upsilon=933 Phi=934 Chi=935 Psi=936 Omega=937 alpha=945 beta=946 gamma=947 delta=948 epsilon=949 zeta=950 " +
            "eta=951 theta=952 iota=953 kappa=954 lambda=955 mu=956 nu=957 xi=958 omicron=959 pi=960 rho=961 sigmaf=962 " +
            "sigma=963 tau=964 upsilon=965 phi=966 chi=967 psi=968 omega=969 thetasym=977 upsih=978 piv=982 ensp=8194 " +
            "emsp=8195 thinsp=8201 zwnj=8204 zwj=8205 lrm=8206 rlm=8207 ndash=8211 mdash=8212 lsquo=8216 rsquo=8217 " +
            "sbquo=8218 ldquo=8220 rdquo=8221 bdquo=8222 dagger=8224 Dagger=8225 bull=8226 hellip=8230 permil=8240 " +
            "prime=8242 Prime=8243 lsaquo=8249 rsaquo=8250 oline=8254 frasl=8260 euro=8364 image=8465 weierp=8472 " +
            "real=8476 trade=8482 alefsym=8501 larr=8592 uarr=8593 rarr=8594 darr=8595 harr=8596 crarr=8629 lArr=8656 " +
            "uArr=8657 rArr=8658 dArr=8659 hArr=8660 forall=8704 part=8706 exist=8707 empty=8709 nabla=8711 isin=8712 " +
            "notin=8713 ni=8715 prod=8719 sum=8721 minus=8722 lowast=8727 radic=8730 prop=8733 infin=8734 ang=8736 " +
            "and=8743 or=8744 cap=8745 cup=8746 int=8747 there4=8756 sim=8764 cong=8773 asymp=8776 ne=8800 equiv=8801 " +
            "le=8804 ge=8805 sub=8834 sup=8835 nsub=8836 sube=8838 supe=8839 oplus=8853 otimes=8855 perp=8869 sdot=8901 " +
            "lceil=8968 rceil=8969 lfloor=8970 rfloor=8971 lang=9001 rang=9002 loz=9674 spades=9824 clubs=9827 hearts=9829 " +
            "diams=9830"
        ).split(' ').associate { pair -> pair.substringBefore('=') to pair.substringAfter('=').toInt() }
}
