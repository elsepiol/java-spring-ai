package dojo.javacore.l2;

import dojo.Kata;

/**
 * KATA javacore L2 "Expr" (tracks/01-java-core/L2-practical.md, section 6: records, sealed, switch).
 *
 * A tiny arithmetic expression tree, the way modern Java models "one of these shapes":
 * a sealed interface with record implementations, and switch with pattern matching.
 * Because the interface is sealed, the compiler knows every case: no default branch needed,
 * and adding a new record makes every incomplete switch a compile error. That is the point.
 *
 * Implement, each with ONE switch over the expression (recursion is fine: it is a tree):
 *   eval:     the value. Division by zero throws ArithmeticException.
 *   show:     fully parenthesised text: Add(Num 1, Mul(Num 2, Num 3)) -> "(1 + (2 * 3))", Neg -> "-x".
 *   simplify: x + 0 -> x, 0 + x -> x, x * 1 -> x, 1 * x -> x, x * 0 -> 0, 0 * x -> 0, -(-x) -> x,
 *             applied bottom-up (simplify the children first).
 */
public sealed interface Expr {

	record Num(long value) implements Expr {
	}

	record Add(Expr left, Expr right) implements Expr {
	}

	record Mul(Expr left, Expr right) implements Expr {
	}

	record Div(Expr left, Expr right) implements Expr {
	}

	record Neg(Expr inner) implements Expr {
	}

	static long eval(Expr e) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 6");
	}

	static String show(Expr e) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 6");
	}

	static Expr simplify(Expr e) {
		return Kata.todo("tracks/01-java-core/L2-practical.md, section 6 (record patterns: case Add(Num(var v), var r))");
	}

}
