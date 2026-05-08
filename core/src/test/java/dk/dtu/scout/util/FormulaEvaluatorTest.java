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
        assertEquals(2.0, FormulaEvaluator.eval("5 - 3", 10), 1e-9);
        assertEquals(6.0, FormulaEvaluator.eval("2 * 3", 10), 1e-9);
    }

    @Test
    void eval_handlesPheromoneBoundFormulasUsedByAco() {
        assertEquals(0.25, FormulaEvaluator.eval("1/n", 4), 1e-9);
        assertEquals(0.75, FormulaEvaluator.eval("1 - 1/n", 4), 1e-9);
        assertEquals(0.5, FormulaEvaluator.eval("0.5", 4), 1e-9);
        assertEquals(0.125, FormulaEvaluator.eval("1 / (2 * n)", 4), 1e-9);
    }

    @Test
    void eval_handlesWhitespaceDecimalsAndUppercaseN() {
        assertEquals(0.5, FormulaEvaluator.eval(" .5 ", 10), 1e-9);
        assertEquals(1.25, FormulaEvaluator.eval("1.25", 10), 1e-9);
        assertEquals(3.5, FormulaEvaluator.eval("1.5 + 2", 10), 1e-9);
        assertEquals(5.0, FormulaEvaluator.eval("N / 2", 10), 1e-9);
    }

    @Test
    void eval_handlesUnaryMinusInDifferentPositions() {
        assertEquals(-2.0, FormulaEvaluator.eval("-2", 10), 1e-9);
        assertEquals(8.0, FormulaEvaluator.eval("10 + -2", 10), 1e-9);
        assertEquals(-5.0, FormulaEvaluator.eval("-(2 + 3)", 10), 1e-9);
    }

    @Test
    void eval_rejectsEmptyFormulaAndInvalidDimension() {
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval(null, 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("   ", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("n", 0));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("n", -1));
    }

    @Test
    void eval_rejectsUnsupportedCharactersAndVariables() {
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 $ 2", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("x", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("abc", 10));
    }

    @Test
    void eval_rejectsUnsupportedFunctionSyntaxAndScientificNotation() {
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("min(n, 3)", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("max(n, 3)", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1e-2", 10));
    }

    @Test
    void eval_rejectsIncompleteExpressions() {
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 +", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("+", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 2", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("()", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 / / 2", 10));
    }

    @Test
    void eval_rejectsMismatchedParenthesesAndComma() {
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("(1 + 2", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 + 2)", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 + 2(", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1 + 2 )", 10));
        assertThrows(IllegalArgumentException.class, () -> FormulaEvaluator.eval("1, 2", 10));
    }
}