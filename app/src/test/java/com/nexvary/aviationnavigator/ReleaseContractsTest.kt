package com.nexvary.aviationnavigator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class ReleaseContractsTest {

    @Test
    fun supportedLanguagesMatchReleaseContract() {
        assertEquals(
            setOf("en", "ar", "tr", "es", "de", "it", "fr", "ur", "fa", "ru"),
            supportedLanguageCodes
        )
    }

    @Test
    fun rtlLanguagesUseRtlLayoutContract() {
        assertTrue(isRtlLanguageCode("ar"))
        assertTrue(isRtlLanguageCode("ur"))
        assertTrue(isRtlLanguageCode("fa"))
        assertFalse(isRtlLanguageCode("en"))
        assertFalse(isRtlLanguageCode("tr"))
    }

    @Test
    fun publicSocialLinksUseSecureSchemesAndEmailUsesMailto() {
        ExternalLinks.webLinks.forEach { link ->
            assertEquals("https", URI(link).scheme)
        }
        assertEquals("mailto", URI(ExternalLinks.EMAIL).scheme)
        assertEquals(5, ExternalLinks.all.size)
    }
}
