package com.youtubetranscript.core

import com.youtubetranscript.models.TranscriptSegment
import com.youtubetranscript.TranscriptException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Parses transcript XML content into segments.
 * 
 * This class handles the XML parsing and HTML entity decoding.
 */
class TranscriptParser(
    private val preserveFormatting: Boolean = false
) {
    
    /**
     * Parse transcript XML content.
     */
    fun parse(xmlContent: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            
            var eventType = parser.eventType
            var currentStart: Double? = null
            var currentDuration: Double? = null
            var currentText = StringBuilder()
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "text" -> {
                                currentStart = parser.getAttributeValue(null, "start")?.toDoubleOrNull()
                                currentDuration = parser.getAttributeValue(null, "dur")?.toDoubleOrNull() ?: 0.0
                                currentText.clear()
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (currentStart != null) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "text" -> {
                                if (currentStart != null && currentDuration != null && currentText.isNotEmpty()) {
                                    val text = if (preserveFormatting) {
                                        currentText.toString().trim()
                                    } else {
                                        decodeHtmlEntities(currentText.toString().trim())
                                    }
                                    
                                    if (text.isNotEmpty()) {
                                        segments.add(
                                            TranscriptSegment(
                                                text = text,
                                                start = currentStart,
                                                duration = currentDuration
                                            )
                                        )
                                    }
                                }
                                currentStart = null
                                currentDuration = null
                                currentText.clear()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            throw TranscriptException("Failed to parse transcript XML: ${e.message}", "")
        }
        
        return segments
    }
    
    /**
     * Decode HTML entities in text.
     * 
     * This method handles common HTML entities similar to Python's html.unescape().
     */
    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&copy;", "©")
            .replace("&reg;", "®")
            .replace("&trade;", "™")
            .replace("&hellip;", "…")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&lsquo;", "'")
            .replace("&rsquo;", "'")
            .replace("&ldquo;", """)
            .replace("&rdquo;", """)
            .replace("&bull;", "•")
            .replace("&middot;", "·")
            .replace("&sect;", "§")
            .replace("&para;", "¶")
            .replace("&euro;", "€")
            .replace("&pound;", "£")
            .replace("&yen;", "¥")
            .replace("&cent;", "¢")
            .replace("&deg;", "°")
            .replace("&plusmn;", "±")
            .replace("&times;", "×")
            .replace("&divide;", "÷")
            .replace("&frac14;", "¼")
            .replace("&frac12;", "½")
            .replace("&frac34;", "¾")
            .replace("&sup1;", "¹")
            .replace("&sup2;", "²")
            .replace("&sup3;", "³")
            .replace("&alpha;", "α")
            .replace("&beta;", "β")
            .replace("&gamma;", "γ")
            .replace("&delta;", "δ")
            .replace("&epsilon;", "ε")
            .replace("&zeta;", "ζ")
            .replace("&eta;", "η")
            .replace("&theta;", "θ")
            .replace("&iota;", "ι")
            .replace("&kappa;", "κ")
            .replace("&lambda;", "λ")
            .replace("&mu;", "μ")
            .replace("&nu;", "ν")
            .replace("&xi;", "ξ")
            .replace("&omicron;", "ο")
            .replace("&pi;", "π")
            .replace("&rho;", "ρ")
            .replace("&sigma;", "σ")
            .replace("&tau;", "τ")
            .replace("&upsilon;", "υ")
            .replace("&phi;", "φ")
            .replace("&chi;", "χ")
            .replace("&psi;", "ψ")
            .replace("&omega;", "ω")
            .replace("&Alpha;", "Α")
            .replace("&Beta;", "Β")
            .replace("&Gamma;", "Γ")
            .replace("&Delta;", "Δ")
            .replace("&Epsilon;", "Ε")
            .replace("&Zeta;", "Ζ")
            .replace("&Eta;", "Η")
            .replace("&Theta;", "Θ")
            .replace("&Iota;", "Ι")
            .replace("&Kappa;", "Κ")
            .replace("&Lambda;", "Λ")
            .replace("&Mu;", "Μ")
            .replace("&Nu;", "Ν")
            .replace("&Xi;", "Ξ")
            .replace("&Omicron;", "Ο")
            .replace("&Pi;", "Π")
            .replace("&Rho;", "Ρ")
            .replace("&Sigma;", "Σ")
            .replace("&Tau;", "Τ")
            .replace("&Upsilon;", "Υ")
            .replace("&Phi;", "Φ")
            .replace("&Chi;", "Χ")
            .replace("&Psi;", "Ψ")
            .replace("&Omega;", "Ω")
    }
}
