package dk.dtu.scout.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormulaEvaluatorTest {

    @Test
    void eval_handlesNumbersVariableAndBasicOperators() {
        assertEquals(5.0, FormulaEvaluator.eval("2 + 3", 10), 1e-9);
        assertEquals(4.0, FormulaEvaluator.eval("n / 2", 8), 1e-9);
        assertEquals(14.0, FormulaEvaluator.eval("2 + 3 * 4", 10), 1e-9);
        assertEquals(20.0, FormulaEvaluator.eval("(2 + 3) * 4", 10), 1e-9);
    }

    @Test
    void eval_handlesMinMaxAndUnaryMinus() {
        assertEquals(3.0, FormulaEvaluator.eval("min(n, 3)", 10), 1e-9);
        assertEquals(10.0, FormulaEvaluator.eval("max(n, 3)", 10), 1e-9);
        assertEquals(-2.0, FormulaEvaluator.eval("-2", 10), 1e-9);
        assertEquals(8.0, FormulaEvaluator.eval("10 + -2", 10), 1e-9);
    }

    @Test
    void eval_handlesScientificNotation() {
        assertEquals(0.01, FormulaEvaluator.eval("1e-2", 10), 1e-12);
        assertEquals(100.0, FormulaEvaluator.eval("1e+2", 10), 1e-9);
    }

    @Test
    void eval_rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval(null, 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("   ", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("n", 0));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("x", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 +", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("(1 + 2", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("min(1,)", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 $ 2", 10));
    }
}