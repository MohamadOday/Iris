/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * Helper class to parse and evaluate mathematical expressions inline within input connections.
 */
public final class InlineMathSolver {

    public static String extractMathExpression(String text) {
        if (text == null || text.isEmpty()) return null;
        int start = text.length() - 1;
        boolean hasOperator = false;
        while (start >= 0) {
            char c = text.charAt(start);
            if (Character.isDigit(c) || c == '.' || c == ' ' || c == '(' || c == ')') {
                if (c == '(' || c == ')') hasOperator = true;
                start--;
            } else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '^') {
                hasOperator = true;
                start--;
            } else {
                break;
            }
        }
        if (!hasOperator) return null;
        String expr = text.substring(start + 1).trim();
        if (expr.isEmpty()) return null;
        return expr;
    }

    public static double evaluateMath(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    }
                    else if (eat('%')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x %= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }

    public static String formatMathResult(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            throw new ArithmeticException("Invalid math result");
        }
        if (val == (long) val) {
            return String.valueOf((long) val);
        }
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.######");
        return df.format(val);
    }

    public static boolean handleInlineMath(InputConnection conn) {
        if (conn == null) return false;
        CharSequence before = conn.getTextBeforeCursor(128, 0);
        if (before == null || before.length() == 0) return false;

        String expr = extractMathExpression(before.toString());
        if (expr == null) return false;

        try {
            double res = evaluateMath(expr);
            String resStr = formatMathResult(res);
            conn.deleteSurroundingText(expr.length(), 0);
            conn.commitText(resStr, 1);
            return true;
        } catch (Exception e) {
            // Not a valid math expression, ignore
        }
        return false;
    }

    public static boolean handleInlineMathForSimulatedInput(EditText activeInput) {
        if (activeInput == null) return false;
        String text = activeInput.getText().toString();
        int selStart = activeInput.getSelectionStart();
        int selEnd = activeInput.getSelectionEnd();
        if (selStart < 0 || selStart != selEnd) return false;

        String before = text.substring(0, selStart);
        String expr = extractMathExpression(before);
        if (expr == null) return false;

        try {
            double res = evaluateMath(expr);
            String resStr = formatMathResult(res);
            String after = text.substring(selStart);
            String newBefore = before.substring(0, before.length() - expr.length()) + resStr;
            activeInput.setText(newBefore + after);
            activeInput.setSelection(newBefore.length());
            return true;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
}
