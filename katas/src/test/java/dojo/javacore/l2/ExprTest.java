package dojo.javacore.l2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.javacore.l2.Expr.Add;
import dojo.javacore.l2.Expr.Div;
import dojo.javacore.l2.Expr.Mul;
import dojo.javacore.l2.Expr.Neg;
import dojo.javacore.l2.Expr.Num;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("javacore")
@Tag("L2")
class ExprTest {

	private static Num n(long v) {
		return new Num(v);
	}

	@Test
	void evaluates() {
		Expr e = new Add(n(1), new Mul(n(2), n(3)));
		assertThat(Expr.eval(e)).isEqualTo(7);
		assertThat(Expr.eval(new Neg(new Div(n(9), n(2))))).isEqualTo(-4);
	}

	@Test
	void divisionByZeroThrows() {
		assertThatThrownBy(() -> Expr.eval(new Div(n(1), new Add(n(1), n(-1))))).isInstanceOf(ArithmeticException.class);
	}

	@Test
	void shows() {
		assertThat(Expr.show(new Add(n(1), new Mul(n(2), n(3))))).isEqualTo("(1 + (2 * 3))");
		assertThat(Expr.show(new Neg(new Div(n(8), n(4))))).isEqualTo("-(8 / 4)");
	}

	@Test
	void simplifiesBottomUp() {
		assertThat(Expr.simplify(new Add(n(0), new Mul(n(5), n(1))))).isEqualTo(n(5));
		assertThat(Expr.simplify(new Mul(new Add(n(2), n(3)), n(0)))).isEqualTo(n(0));
		assertThat(Expr.simplify(new Neg(new Neg(n(4))))).isEqualTo(n(4));
		assertThat(Expr.simplify(new Add(n(2), new Mul(n(1), new Add(n(0), n(3)))))).isEqualTo(new Add(n(2), n(3)));
		assertThat(Expr.simplify(new Div(n(6), new Mul(n(1), n(2))))).isEqualTo(new Div(n(6), n(2)));
	}

	@Test
	void recordsAreValues() {
		assertThat(new Add(n(1), n(2))).isEqualTo(new Add(n(1), n(2))).hasToString("Add[left=Num[value=1], right=Num[value=2]]");
	}

}
