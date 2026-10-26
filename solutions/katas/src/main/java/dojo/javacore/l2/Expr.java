package dojo.javacore.l2;

/** Reference solution. Note: no default branches; the sealed hierarchy makes each switch exhaustive. */
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
		return switch (e) {
			case Num n -> n.value();
			case Add(Expr l, Expr r) -> eval(l) + eval(r);
			case Mul(Expr l, Expr r) -> eval(l) * eval(r);
			case Div(Expr l, Expr r) -> eval(l) / eval(r); // long division by zero throws ArithmeticException
			case Neg(Expr inner) -> -eval(inner);
		};
	}

	static String show(Expr e) {
		return switch (e) {
			case Num n -> Long.toString(n.value());
			case Add(Expr l, Expr r) -> "(" + show(l) + " + " + show(r) + ")";
			case Mul(Expr l, Expr r) -> "(" + show(l) + " * " + show(r) + ")";
			case Div(Expr l, Expr r) -> "(" + show(l) + " / " + show(r) + ")";
			case Neg(Expr inner) -> "-" + show(inner);
		};
	}

	static Expr simplify(Expr e) {
		return switch (e) {
			case Num n -> n;
			case Add(Expr l, Expr r) -> simplifyAdd(simplify(l), simplify(r));
			case Mul(Expr l, Expr r) -> simplifyMul(simplify(l), simplify(r));
			case Div(Expr l, Expr r) -> new Div(simplify(l), simplify(r));
			case Neg(Expr inner) -> {
				Expr simplified = simplify(inner);
				yield simplified instanceof Neg(Expr x) ? x : new Neg(simplified);
			}
		};
	}

	private static Expr simplifyAdd(Expr l, Expr r) {
		if (l instanceof Num(long v) && v == 0) {
			return r;
		}
		if (r instanceof Num(long v) && v == 0) {
			return l;
		}
		return new Add(l, r);
	}

	private static Expr simplifyMul(Expr l, Expr r) {
		if (l instanceof Num(long v) && v == 0 || r instanceof Num(long w) && w == 0) {
			return new Num(0);
		}
		if (l instanceof Num(long v) && v == 1) {
			return r;
		}
		if (r instanceof Num(long v) && v == 1) {
			return l;
		}
		return new Mul(l, r);
	}

}
