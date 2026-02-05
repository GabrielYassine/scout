package dk.dtu.scout.backend.util;

import java.util.*;

public final class FormulaEvaluator {

    private FormulaEvaluator() {}

    /** Evaluate an expression using only the variable n. */
    public static double eval(String expr, int n) {
        if (expr == null || expr.isBlank()) {
            throw new IllegalArgumentException("Empty formula");
        }
        if (n <= 0) {
            throw new IllegalArgumentException("n must be > 0");
        }

        List<String> tokens = tokenize(expr);
        List<String> rpn = toRpn(tokens);
        return evalRpn(rpn, n);
    }

    private static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        int i = 0;

        while (i < s.length()) {
            char c = s.charAt(i);

            if (Character.isWhitespace(c)) { i++; continue; }

            // number: 12, 12.3, .5, 1e-3
            if (Character.isDigit(c) || c == '.') {
                int start = i;
                i++;
                while (i < s.length()) {
                    char d = s.charAt(i);
                    if (Character.isDigit(d) || d == '.' || d == 'e' || d == 'E' || d == '+' || d == '-') {
                        // allow + or - only right after e/E
                        if ((d == '+' || d == '-') && !(s.charAt(i - 1) == 'e' || s.charAt(i - 1) == 'E')) break;
                        i++;
                    } else break;
                }
                out.add(s.substring(start, i));
                continue;
            }

            // identifier: n, min, max
            if (Character.isLetter(c)) {
                int start = i;
                i++;
                while (i < s.length() && Character.isLetterOrDigit(s.charAt(i))) i++;
                out.add(s.substring(start, i));
                continue;
            }

            // operators / punctuation
            if ("+-*/(),".indexOf(c) >= 0) {
                out.add(String.valueOf(c));
                i++;
                continue;
            }

            throw new IllegalArgumentException("Invalid character in formula: '" + c + "'");
        }

        // handle unary minus: convert "-x" to "0 - x"
        List<String> fixed = new ArrayList<>();
        for (int j = 0; j < out.size(); j++) {
            String t = out.get(j);
            if (t.equals("-")) {
                String prev = (j == 0) ? null : out.get(j - 1);
                if (j == 0 || isOperator(prev) || prev.equals("(") || prev.equals(",")) {
                    fixed.add("0");
                    fixed.add("-");
                    continue;
                }
            }
            fixed.add(t);
        }
        return fixed;
    }

    private static boolean isOperator(String t) {
        return t != null && (t.equals("+") || t.equals("-") || t.equals("*") || t.equals("/"));
    }

    private static int precedence(String op) {
        return (op.equals("*") || op.equals("/")) ? 2 : 1;
    }

    private static boolean isFunction(String t) {
        return t.equalsIgnoreCase("min") || t.equalsIgnoreCase("max");
    }

    private static List<String> toRpn(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();

        for (String t : tokens) {
            if (isNumber(t) || isIdentifier(t)) {
                output.add(t);
            } else if (isFunction(t)) {
                stack.push(t);
            } else if (t.equals(",")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) throw new IllegalArgumentException("Misplaced comma");
            } else if (isOperator(t)) {
                while (!stack.isEmpty() && isOperator(stack.peek())
                        && precedence(stack.peek()) >= precedence(t)) {
                    output.add(stack.pop());
                }
                stack.push(t);
            } else if (t.equals("(")) {
                stack.push(t);
            } else if (t.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses");
                stack.pop(); // pop "("
                if (!stack.isEmpty() && isFunction(stack.peek())) {
                    output.add(stack.pop()); // function call
                }
            } else {
                throw new IllegalArgumentException("Unknown token: " + t);
            }
        }

        while (!stack.isEmpty()) {
            String t = stack.pop();
            if (t.equals("(") || t.equals(")")) throw new IllegalArgumentException("Mismatched parentheses");
            output.add(t);
        }
        return output;
    }

    private static double evalRpn(List<String> rpn, int n) {
        Deque<Double> st = new ArrayDeque<>();

        for (String t : rpn) {
            if (isNumberToken(t)) {
                st.push(Double.parseDouble(t));
            } else if (isFunction(t)) {
                double b = popOrFail(st, t);
                double a = popOrFail(st, t);
                if (t.equalsIgnoreCase("min")) st.push(Math.min(a, b));
                else st.push(Math.max(a, b));
            } else if (isOperator(t)) {
                double b = popOrFail(st, t);
                double a = popOrFail(st, t);
                switch (t) {
                    case "+" -> st.push(a + b);
                    case "-" -> st.push(a - b);
                    case "*" -> st.push(a * b);
                    case "/" -> st.push(a / b);
                    default -> throw new IllegalStateException("Unexpected operator: " + t);
                }
            } else {
                // only allowed variable is "n"
                if (!t.equalsIgnoreCase("n")) {
                    throw new IllegalArgumentException("Unknown variable: " + t + " (only 'n' is supported)");
                }
                st.push((double) n);
            }
        }

        if (st.size() != 1) throw new IllegalArgumentException("Invalid expression");
        return st.pop();
    }

    private static double popOrFail(Deque<Double> st, String ctx) {
        if (st.isEmpty()) throw new IllegalArgumentException("Invalid expression near: " + ctx);
        return st.pop();
    }

    private static boolean isIdentifier(String t) {
        return Character.isLetter(t.charAt(0)) && !isFunction(t);
    }

    private static boolean isNumber(String t) {
        return isNumberToken(t);
    }

    private static boolean isNumberToken(String t) {
        try {
            Double.parseDouble(t);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
