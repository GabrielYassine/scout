package dk.dtu.scout.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 *
 * @author s230632
 */
public final class FormulaEvaluator {

    private FormulaEvaluator() {}

    public static double eval(String expression, int n) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Formula cannot be empty");
        }

        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }

        List<String> tokens = tokenize(expression);
        List<String> rpn = toRpn(tokens);
        return evalRpn(rpn, n);
    }

    private static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        int i = 0;

        while (i < expression.length()) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                int start = i++;

                while (i < expression.length()) {
                    char next = expression.charAt(i);

                    if (!Character.isDigit(next) && next != '.') {
                        break;
                    }

                    i++;
                }

                tokens.add(expression.substring(start, i));
            } else if (c == 'n' || c == 'N') {
                tokens.add("n");
                i++;
            } else if (isOperator(c) || c == '(' || c == ')') {
                tokens.add(String.valueOf(c));
                i++;
            } else {
                throw new IllegalArgumentException("Invalid character in formula: '" + c + "'");
            }
        }

        return handleUnaryMinus(tokens);
    }

    private static List<String> handleUnaryMinus(List<String> tokens) {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (token.equals("-") && isUnaryMinus(tokens, i)) {
                result.add("0");
                result.add("-");
            } else {
                result.add(token);
            }
        }

        return result;
    }

    private static boolean isUnaryMinus(List<String> tokens, int index) {
        if (index == 0) {
            return true;
        }

        String previous = tokens.get(index - 1);
        return isOperator(previous) || previous.equals("(");
    }

    private static List<String> toRpn(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();

        for (String token : tokens) {
            if (isNumber(token) || token.equals("n")) {
                output.add(token);
            } else if (isOperator(token)) {
                while (!stack.isEmpty() && isOperator(stack.peek())) {
                    assert stack.peek() != null;
                    if (!(precedence(stack.peek()) >= precedence(token))) break;
                    output.add(stack.pop());
                }

                stack.push(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }

                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Mismatched parentheses");
                }

                stack.pop();
            }
        }

        while (!stack.isEmpty()) {
            String token = stack.pop();

            if (token.equals("(")) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }

            output.add(token);
        }

        return output;
    }

    private static double evalRpn(List<String> rpn, int n) {
        Deque<Double> stack = new ArrayDeque<>();

        for (String token : rpn) {
            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (token.equals("n")) {
                stack.push((double) n);
            } else {
                applyOperator(token, stack);
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }

        return stack.pop();
    }

    private static void applyOperator(String operator, Deque<Double> stack) {
        double b = popOrFail(stack, operator);
        double a = popOrFail(stack, operator);

        switch (operator) {
            case "+" -> stack.push(a + b);
            case "-" -> stack.push(a - b);
            case "*" -> stack.push(a * b);
            case "/" -> stack.push(a / b);
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    private static double popOrFail(Deque<Double> stack, String context) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Invalid expression near: " + context);
        }

        return stack.pop();
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private static boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
    }

    private static int precedence(String operator) {
        if (operator.equals("*") || operator.equals("/")) {
            return 2;
        }

        return 1;
    }

    private static boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}