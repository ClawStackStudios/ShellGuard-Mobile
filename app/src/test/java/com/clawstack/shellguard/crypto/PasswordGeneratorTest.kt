package com.clawstack.shellguard.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun testDefaultPasswordLengthAndCharacters() {
        val options = PasswordOptions(length = 20)
        val password = PasswordGenerator.generatePassword(options)

        assertEquals(20, password.length)
        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun testAvoidAmbiguousCharacters() {
        val options = PasswordOptions(length = 50, avoidAmbiguous = true)
        val password = PasswordGenerator.generatePassword(options)

        val ambiguous = "0O1lI|"
        assertFalse(password.any { it in ambiguous })
    }

    @Test
    fun testNumbersOnly() {
        val options = PasswordOptions(
            length = 15,
            includeUppercase = false,
            includeLowercase = false,
            includeNumbers = true,
            includeSymbols = false,
            avoidAmbiguous = false
        )
        val password = PasswordGenerator.generatePassword(options)
        assertEquals(15, password.length)
        assertTrue(password.all { it.isDigit() })
    }

    @Test
    fun testPassphraseGeneration() {
        val options = PassphraseOptions(
            wordCount = 4,
            separator = "-",
            capitalize = true,
            includeNumber = false
        )
        val phrase = PasswordGenerator.generatePassphrase(options)
        val words = phrase.split("-")

        assertEquals(4, words.size)
        assertTrue(words.all { it[0].isUpperCase() })
    }

    @Test
    fun testPassphraseWithNumber() {
        val options = PassphraseOptions(
            wordCount = 5,
            separator = "_",
            includeNumber = true
        )
        val phrase = PasswordGenerator.generatePassphrase(options)
        val words = phrase.split("_")

        assertEquals(5, words.size)
        assertTrue(phrase.any { it.isDigit() })
    }

    @Test
    fun testStrengthEvaluation() {
        assertEquals(PasswordStrength.VERY_WEAK, PasswordGenerator.evaluateStrength("short"))
        assertEquals(PasswordStrength.WEAK, PasswordGenerator.evaluateStrength("password"))
        assertTrue(PasswordGenerator.evaluateStrength("Correct-Horse-Battery-Staple-99!").score >= 4)
        assertEquals(
            PasswordStrength.VERY_STRONG,
            PasswordGenerator.evaluateStrength("vP8#mK9!zL2@wQ4" + '$' + "xR7^yN3*")
        )
    }
}
