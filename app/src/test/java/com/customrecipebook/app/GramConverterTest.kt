package com.customrecipebook.app

import com.customrecipebook.app.domain.GramConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GramConverterTest {
    @Test
    fun cupsOfFlourUseTypicalDensity() {
        val result = GramConverter.toGrams(2.0, "cups", "all-purpose flour")
        assertNotNull(result)
        assertEquals(240.0, result!!.grams, 0.01)
        assertTrue(result.approximate)
    }

    @Test
    fun tbspOilConverts() {
        val result = GramConverter.toGrams(1.0, "tbsp", "olive oil")
        assertNotNull(result)
        assertEquals(14.0, result!!.grams, 0.01)
        assertTrue(result.approximate)
    }

    @Test
    fun ouncesAreExactWeight() {
        val result = GramConverter.toGrams(8.0, "oz", "chicken")
        assertNotNull(result)
        assertEquals(227.0, result!!.grams, 0.5)
        assertFalse(result.approximate)
    }

    @Test
    fun countsDoNotConvert() {
        assertNull(GramConverter.toGrams(2.0, "", "eggs"))
        assertNull(GramConverter.toGrams(2.0, "large", "eggs"))
        assertNull(GramConverter.toGrams(1.0, "pinch", "salt"))
    }
}
