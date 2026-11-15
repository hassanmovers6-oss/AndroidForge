package com.educationwithhassan.financetools

import com.educationwithhassan.financetools.domain.engine.FinancialEngine
import com.educationwithhassan.financetools.domain.model.InputKind
import com.educationwithhassan.financetools.domain.model.ToolCatalog
import org.junit.Assert.*
import org.junit.Test

class FinancialEngineTest {

    @Test
    fun catalogHasAll120UniqueTools() {
        val all = ToolCatalog.allTools
        assertEquals("Catalog must contain exactly 120 tools", 120, all.size)
        assertEquals(120, all.map { it.id }.toSet().size)
        assertEquals((1..120).toList(), all.map { it.number })
    }

    @Test
    fun all120CalculatorsHandleNormalZeroInvalidAndBoundaryInputs() {
        for (tool in ToolCatalog.allTools) {
            val defaults = tool.fields.associate { it.key to it.defaultValue }

            // 1. Normal calculation
            val normal = FinancialEngine.calculate(tool.id, defaults, tool.defaultRows, "USD")
            assertTrue("Tool ${tool.number} (${tool.id}) failed normal calc: ${normal.errorMessage}", normal.valid)
            assertTrue("NaN/Inf in ${tool.id}", normal.primaryValue.isFinite())
            assertFalse("NaN text in ${tool.id}", normal.primaryFormatted.contains("NaN"))

            // 2. Zero values
            val zeros = defaults.toMutableMap()
            tool.fields.filter { it.kind == InputKind.CURRENCY || it.kind == InputKind.NUMBER || it.kind == InputKind.PERCENT }
                .forEach { zeros[it.key] = "0" }
            val zeroRes = FinancialEngine.calculate(tool.id, zeros, tool.defaultRows, "USD")
            if (zeroRes.valid) {
                assertTrue("Zero caused non-finite in ${tool.id}", zeroRes.primaryValue.isFinite())
            } else {
                assertNotNull("Zero error message missing for ${tool.id}", zeroRes.errorMessage)
            }

            // 3. Invalid non-numeric input
            val numField = tool.fields.firstOrNull { it.kind == InputKind.CURRENCY || it.kind == InputKind.NUMBER || it.kind == InputKind.PERCENT }
            if (numField != null) {
                val bad = defaults.toMutableMap().apply { put(numField.key, "invalid_abc") }
                val badRes = FinancialEngine.calculate(tool.id, bad, tool.defaultRows, "USD")
                assertFalse("Invalid input accepted in ${tool.id}", badRes.valid)
                assertNotNull(badRes.errorMessage)
            }

            // 4. Boundary large values
            val boundary = defaults.toMutableMap()
            tool.fields.filter { it.kind == InputKind.CURRENCY }.forEach { boundary[it.key] = "1000000" }
            tool.fields.filter { it.kind == InputKind.PERCENT }.forEach { boundary[it.key] = "0" }
            val boundRes = FinancialEngine.calculate(tool.id, boundary, tool.defaultRows, "USD")
            if (boundRes.valid) {
                assertTrue("Boundary caused non-finite in ${tool.id}", boundRes.primaryValue.isFinite())
            }
        }
    }

    @Test
    fun verifyCoreFinancialFormulasAccuracy() {
        val si = FinancialEngine.calculate(
            "simple-interest-calculator",
            mapOf("principal" to "10000", "rate" to "5", "time" to "2", "unit" to "years")
        )
        assertTrue(si.valid)
        assertEquals(1000.0, si.primaryValue, 0.01)

        val rule72 = FinancialEngine.calculate("rule-of-72-calculator", mapOf("rate" to "8", "principal" to "10000"))
        assertTrue(rule72.valid)
        assertEquals(9.0, rule72.primaryValue, 0.01)

        val roi = FinancialEngine.calculate("roi-calculator", mapOf("initial" to "10000", "finalValue" to "15000", "years" to "2"))
        assertTrue(roi.valid)
        assertEquals(50.0, roi.primaryValue, 0.01)
    }
}
