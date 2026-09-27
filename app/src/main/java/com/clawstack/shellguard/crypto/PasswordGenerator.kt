package com.clawstack.shellguard.crypto

import java.security.SecureRandom
import kotlin.math.log2

enum class PasswordStrength(val label: String, val score: Int) {
    VERY_WEAK("Very Weak", 1),
    WEAK("Weak", 2),
    FAIR("Fair", 3),
    STRONG("Strong", 4),
    VERY_STRONG("Very Strong", 5)
}

data class PasswordOptions(
    val length: Int = 20,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val avoidAmbiguous: Boolean = true
)

data class PassphraseOptions(
    val wordCount: Int = 4,
    val separator: String = "-",
    val capitalize: Boolean = true,
    val includeNumber: Boolean = true
)

object PasswordGenerator {

    private val random = SecureRandom()

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    private const val AMBIGUOUS = "0O1lI|"

    private val WORDLIST = listOf(
        "anchor", "beacon", "bioluminescence", "breeze", "calm", "carapace", "cavern", "channel",
        "claw", "cliff", "coral", "current", "depths", "drift", "dune", "ebb", "ember", "estuary",
        "fathom", "fin", "flare", "float", "flow", "foam", "gale", "glimmer", "grotto", "harbor",
        "horizon", "inlet", "island", "kelp", "lagoon", "lantern", "ledge", "light", "lobster",
        "lunar", "marine", "marsh", "mast", "mist", "nadir", "nautical", "nautilus", "navigator",
        "nebula", "ocean", "octopus", "outcrop", "oyster", "passage", "pearl", "pelican", "pier",
        "pilot", "plankton", "polar", "pulse", "quarry", "quay", "quiver", "radiance", "reef",
        "ripple", "rover", "sailor", "sand", "scallop", "sea", "seabird", "seamount", "shadow",
        "shallow", "shell", "shoal", "shore", "siren", "solstice", "spark", "sponge", "spray",
        "starfish", "strait", "strand", "stratum", "subtle", "surge", "swell", "tide", "titan",
        "trench", "trident", "tsunami", "undertow", "urchin", "valve", "vapor", "vent", "vessel",
        "vortex", "voyage", "wave", "wharf", "whirl", "wind", "zenith", "zephyr"
    )

    fun generatePassword(options: PasswordOptions): String {
        var charPool = ""
        val requiredChars = mutableListOf<Char>()

        var upper = UPPERCASE
        var lower = LOWERCASE
        var numbers = NUMBERS
        var symbols = SYMBOLS

        if (options.avoidAmbiguous) {
            upper = upper.filter { it !in AMBIGUOUS }
            lower = lower.filter { it !in AMBIGUOUS }
            numbers = numbers.filter { it !in AMBIGUOUS }
            symbols = symbols.filter { it !in AMBIGUOUS }
        }

        if (options.includeUppercase) {
            charPool += upper
            requiredChars.add(upper[random.nextInt(upper.length)])
        }
        if (options.includeLowercase) {
            charPool += lower
            requiredChars.add(lower[random.nextInt(lower.length)])
        }
        if (options.includeNumbers) {
            charPool += numbers
            requiredChars.add(numbers[random.nextInt(numbers.length)])
        }
        if (options.includeSymbols) {
            charPool += symbols
            requiredChars.add(symbols[random.nextInt(symbols.length)])
        }

        if (charPool.isEmpty()) {
            charPool = lower
            requiredChars.add(lower[random.nextInt(lower.length)])
        }

        val effectiveLength = options.length.coerceAtLeast(requiredChars.size)
        val passwordChars = ArrayList<Char>(effectiveLength)
        passwordChars.addAll(requiredChars)

        for (i in requiredChars.size until effectiveLength) {
            passwordChars.add(charPool[random.nextInt(charPool.length)])
        }

        // Fisher-Yates shuffle
        for (i in passwordChars.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }

        return passwordChars.joinToString("")
    }

    fun generatePassphrase(options: PassphraseOptions): String {
        val count = options.wordCount.coerceIn(3, 8)
        val selectedWords = ArrayList<String>(count)

        for (i in 0 until count) {
            var word = WORDLIST[random.nextInt(WORDLIST.size)]
            if (options.capitalize) {
                word = word.replaceFirstChar { it.uppercase() }
            }
            selectedWords.add(word)
        }

        if (options.includeNumber && selectedWords.isNotEmpty()) {
            val randomDigit = random.nextInt(100).toString()
            val targetIdx = random.nextInt(selectedWords.size)
            selectedWords[targetIdx] = selectedWords[targetIdx] + randomDigit
        }

        return selectedWords.joinToString(options.separator)
    }

    fun evaluateStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.VERY_WEAK

        var poolSize = 0
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        if (poolSize == 0) return PasswordStrength.VERY_WEAK

        val entropy = password.length * log2(poolSize.toDouble())

        return when {
            entropy < 35 -> PasswordStrength.VERY_WEAK
            entropy < 50 -> PasswordStrength.WEAK
            entropy < 65 -> PasswordStrength.FAIR
            entropy < 80 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
}
