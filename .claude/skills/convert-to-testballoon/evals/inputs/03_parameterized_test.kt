package com.example.test

import com.example.StringValidator
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class StringValidatorTest(
    private val input: String,
    private val expectedValid: Boolean,
    private val description: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{2}")
        fun data() = listOf(
            arrayOf("hello@example.com", true, "valid email"),
            arrayOf("not-an-email", false, "missing @ symbol"),
            arrayOf("", false, "empty string"),
            arrayOf("user@", false, "missing domain"),
        )
    }

    @Test
    fun validate_email() {
        assertEquals(expectedValid, StringValidator.isValidEmail(input))
    }
}
