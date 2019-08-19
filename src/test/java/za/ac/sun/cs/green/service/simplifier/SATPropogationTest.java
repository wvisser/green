package za.ac.sun.cs.green.service.simplifier;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.util.Configuration;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SATPropogationTest {

	public static Green solver;

	@BeforeClass
	public static void initialize() {
		solver = new Green();
		Properties props = new Properties();
		props.setProperty("green.services", "sat");
		props.setProperty("green.service.sat", "(simplify sink)");
		props.setProperty("green.service.sat.simplify",
				"za.ac.sun.cs.green.service.simplifier.SATPropagationService");
		props.setProperty("green.service.sat.sink",
				"za.ac.sun.cs.green.service.sink.SinkService");
		Configuration config = new Configuration(solver, props);
		config.configure();
	}

	private void check(Expression expression, String expected) {
		Instance i = new Instance(solver, null, null, expression);
		Object result = i.request("sat");
		assertNotNull(result);
		assertEquals(Instance.class, result.getClass());
		Instance j = (Instance) result;
		assertEquals(expected, j.getFullExpression().toString());
	}

	@AfterClass
	public static void report() {
		if (solver != null)
			solver.report();
	}

	@Test
	public void test01() {
		IntVariable v1 = new IntVariable("aa", 0, 99);
		IntConstant c1 = new IntConstant(0);
		IntVariable v2 = new IntVariable("bb", 0, 99);
		IntConstant c2 = new IntConstant(0);
		Operation o1 = new Operation(Operation.Operator.EQ, v1, c1);
		Operation o2 = new Operation(Operation.Operator.LE, v2, c2);
		Operation o = new Operation(Operation.Operator.AND, o1, o2);
		System.out.println("operation = " + o);
		check(o, "(bb<=0)&&(aa==0)");
	}

	@Test
	public void test02() {
		IntVariable v1 = new IntVariable("aa", 0, 99);
		IntConstant c1 = new IntConstant(0);
		IntVariable v2 = new IntVariable("bb", 0, 99);
		IntConstant c2 = new IntConstant(1);
		Operation o1 = new Operation(Operation.Operator.EQ, v1, c1);
		Operation o2 = new Operation(Operation.Operator.EQ, v2, c2);
		Operation o = new Operation(Operation.Operator.AND, o1, o2);
		System.out.println("operation = " + o);
		check(o, "(aa==0)&&(bb==1)");
	}

	@Test
	public void test03() {
		IntVariable v1 = new IntVariable("aa", 0, 99);
		IntConstant c1 = new IntConstant(0);
		IntVariable v2 = new IntVariable("bb", 0, 99);
		IntConstant c2 = new IntConstant(1);
		IntVariable v3 = new IntVariable("cc", 0, 99);
		Operation o1 = new Operation(Operation.Operator.EQ, v1, c1);
		Operation o2 = new Operation(Operation.Operator.EQ, v2, c2);
		Operation o3 = new Operation(Operation.Operator.AND, o1, o2);
		Operation o4 = new Operation(Operation.Operator.ADD, v1, v2);
		Operation o5 = new Operation(Operation.Operator.EQ, v3, o4);
		Operation o = new Operation(Operation.Operator.AND, o3, o5);
		System.out.println("operation = " + o);
		check(o, "((aa==0)&&(bb==1))&&(cc==1)");
	}

	@Test
	public void test04() {
		// aa = 0
		// bb = 1
		// cc = aa + bb
		// cc >= 3
		IntVariable v1 = new IntVariable("aa", 0, 99);
		IntConstant c1 = new IntConstant(0);
		IntVariable v2 = new IntVariable("bb", 0, 99);
		IntConstant c2 = new IntConstant(1);
		IntConstant c3 = new IntConstant(3);
		IntVariable v3 = new IntVariable("cc", 0, 99);
		Operation o1 = new Operation(Operation.Operator.EQ, v1, c1);
		Operation o2 = new Operation(Operation.Operator.EQ, v2, c2);
		Operation o3 = new Operation(Operation.Operator.AND, o1, o2);
		Operation o4 = new Operation(Operation.Operator.ADD, v1, v2);
		Operation o5 = new Operation(Operation.Operator.EQ, v3, o4);
		Operation o6 = new Operation(Operation.Operator.GE, v3, c3);
		Operation o7 = new Operation(Operation.Operator.AND, o3, o5);
		Operation o = new Operation(Operation.Operator.AND, o6, o7);
		System.out.println("operation = " + o);
		check(o, "(((0==1)&&(aa==0))&&(bb==1))&&(cc==1)");
	}

	@Test
	public void test05() {
		// aa = 0
		// aa <= 3
		IntVariable v1 = new IntVariable("aa", 0, 99);
		IntConstant c1 = new IntConstant(0);
		IntConstant c2 = new IntConstant(3);
		Operation o1 = new Operation(Operation.Operator.EQ, v1, c1);
		Operation o2 = new Operation(Operation.Operator.LE, v1, c2);
		Operation o = new Operation(Operation.Operator.AND, o1, o2);
		System.out.println("operation = " + o);
		check(o, "aa==0");
	}

	@Test
	public void test06() {
		// x = 7
		// y = 7 - x
		// z = y * (28 + x - 2)
		IntVariable v1 = new IntVariable("x", 0, 99);
		IntVariable v2 = new IntVariable("y", 0, 99);
		IntVariable v3 = new IntVariable("z", 0, 99);

		IntConstant c1 = new IntConstant(7);
		IntConstant c2 = new IntConstant(28);
		IntConstant c3 = new IntConstant(2);

		Operation o1 = new Operation(Operation.Operator.EQ, v1, c1);
		Operation o2 = new Operation(Operation.Operator.SUB, c1, v1);
		Operation o3 = new Operation(Operation.Operator.EQ, v2, o2);
		Operation o4 = new Operation(Operation.Operator.ADD, c2, v1);
		Operation o5 = new Operation(Operation.Operator.SUB, o4, c3);
		Operation o6 = new Operation(Operation.Operator.MUL, v2, o5);
		Operation o7 = new Operation(Operation.Operator.EQ, v3, o6);

		Operation oo = new Operation(Operation.Operator.AND, o1, o3);
		Operation o = new Operation(Operation.Operator.AND, oo, o7);
		System.out.println("operation = " + o);
		check(o, "((x==7)&&(y==0))&&(z==0)");
	}

	@Test
	public void test07() {
		// x = 7
		// y = 7 - x
		// z = (y * 28) + (x - 2)
		IntVariable v1 = new IntVariable("x", 0, 99);
		IntVariable v2 = new IntVariable("y", 0, 99);
		IntVariable v3 = new IntVariable("z", 0, 99);

		IntConstant c1 = new IntConstant(7);
		IntConstant c2 = new IntConstant(28);
		IntConstant c3 = new IntConstant(2);

		Operation o1 = new Operation(Operation.Operator.EQ, v1, c1);

		Operation o2 = new Operation(Operation.Operator.SUB, c1, v1);
		Operation o3 = new Operation(Operation.Operator.EQ, v2, o2);

		Operation o4 = new Operation(Operation.Operator.MUL, v2, c2);
		Operation o5 = new Operation(Operation.Operator.SUB, v1, c3);
		Operation o6 = new Operation(Operation.Operator.ADD, o4, o5);
		Operation o7 = new Operation(Operation.Operator.EQ, v3, o6);

		Operation oo = new Operation(Operation.Operator.AND, o1, o3);
		Operation o = new Operation(Operation.Operator.AND, oo, o7);
		System.out.println("operation = " + o);
		check(o, "((x==7)&&(y==0))&&(z==5)");
	}

	@Test
	public void test08() {
		// a = 30
		// b = 37 - (a + 2)
		// ## c = b * 4
		// c = b * 4 - 10
		// d = c * (60 - a)
		IntVariable v1 = new IntVariable("a", 0, 99);
		IntVariable v2 = new IntVariable("b", 0, 99);
		IntVariable v3 = new IntVariable("c", 0, 99);
		IntVariable v4 = new IntVariable("d", 0, 99);

		IntConstant c1 = new IntConstant(30);
		IntConstant c2 = new IntConstant(37);
		IntConstant c3 = new IntConstant(2);
		IntConstant c4 = new IntConstant(4);
		IntConstant c5 = new IntConstant(10);
		IntConstant c6 = new IntConstant(60);

		Operation o1 = new Operation(Operation.Operator.EQ, v1, c1);

		Operation o2 = new Operation(Operation.Operator.ADD, v1, c3);
		Operation o3 = new Operation(Operation.Operator.SUB, c2, o2);
		Operation o4 = new Operation(Operation.Operator.EQ, v2, o3);

		Operation o5 = new Operation(Operation.Operator.MUL, v2, c4);
		Operation o7 = new Operation(Operation.Operator.SUB, o5, c5);
		Operation o6 = new Operation(Operation.Operator.EQ, v3, o7);

//        Operation o8 = new Operation(Operation.Operator.EQ, v3, o7);

		Operation o9 = new Operation(Operation.Operator.SUB, c6, v1);
		Operation o10 = new Operation(Operation.Operator.MUL, v3, o9);
		Operation o11 = new Operation(Operation.Operator.EQ, v4, o10);

		Operation ooo = new Operation(Operation.Operator.AND, o1, o4);
		Operation oo = new Operation(Operation.Operator.AND, ooo, o6);
		Operation o0 = new Operation(Operation.Operator.AND, oo, o11);
		System.out.println("operation = " + o0);
		check(o0, "(((a==30)&&(b==5))&&(c==10))&&(d==300)");
	}

	@Test
	public void test09() {
		// a = b
		// c = 3 + a
		IntVariable v1 = new IntVariable("a", 0, 99);
		IntVariable v2 = new IntVariable("b", 0, 99);
		IntVariable v3 = new IntVariable("c", 0, 99);
		IntConstant c1 = new IntConstant(3);
		Operation o1 = new Operation(Operation.Operator.EQ, v1, v2);

		Operation o2 = new Operation(Operation.Operator.ADD, c1, v1);
		Operation o3 = new Operation(Operation.Operator.EQ, v3, o2);

		Operation o4 = new Operation(Operation.Operator.AND, o1, o3);
		System.out.println("operation = " + o4);
		check(o4, "(c==(3+b))&&(a==b)");
	}

	@Test
	public void test10() {
		// a = b
		// b = 5
		// c = 3 + a
		// d = c + b
		// e = f + c + g + 5
		// f = g - a + b - c
		// g = a - b
		IntVariable v1 = new IntVariable("a", 0, 99);
		IntVariable v2 = new IntVariable("b", 0, 99);
		IntVariable v3 = new IntVariable("c", 0, 99);
		IntVariable v4 = new IntVariable("d", 0, 99);
		IntVariable v5 = new IntVariable("e", 0, 99);
		IntVariable v6 = new IntVariable("f", 0, 99);
		IntVariable v7 = new IntVariable("g", 0, 99);

		IntConstant c1 = new IntConstant(3);
		IntConstant c2 = new IntConstant(5);

		Operation eq1 = new Operation(Operation.Operator.EQ, v1, v2);
		Operation eq2 = new Operation(Operation.Operator.EQ, v2, c2);

		Operation o1 = new Operation(Operation.Operator.ADD, c1, v1);
		Operation eq3 = new Operation(Operation.Operator.EQ, v3, o1);

		Operation o2 = new Operation(Operation.Operator.ADD, v3, v2);
		Operation eq4 = new Operation(Operation.Operator.EQ, v4, o2);

		Operation o3 = new Operation(Operation.Operator.ADD, v6, v3);
		Operation o4 = new Operation(Operation.Operator.ADD, v7, c2);
		Operation o5 = new Operation(Operation.Operator.ADD, o3, o4);
		Operation eq5 = new Operation(Operation.Operator.EQ, v5, o5);

		Operation o6 = new Operation(Operation.Operator.SUB, v7, v1);
		Operation o7 = new Operation(Operation.Operator.SUB, v2, v3);
		Operation o8 = new Operation(Operation.Operator.ADD, o6, o7);
		Operation eq6 = new Operation(Operation.Operator.EQ, v6, o8);

		Operation o9 = new Operation(Operation.Operator.SUB, v1, v2);
		Operation eq7 = new Operation(Operation.Operator.EQ, v7, o9);

		Operation phi1 = new Operation(Operation.Operator.AND, eq1, eq2);
		Operation phi2 = new Operation(Operation.Operator.AND, phi1, eq3);
		Operation phi3 = new Operation(Operation.Operator.AND, phi2, eq4);
		Operation phi4 = new Operation(Operation.Operator.AND, phi3, eq5);
		Operation phi5 = new Operation(Operation.Operator.AND, phi4, eq6);
		Operation phi6 = new Operation(Operation.Operator.AND, phi5, eq7);

		System.out.println("operation = " + phi6);
		check(phi6, "((((((a==b)&&(b==5))&&(c==8))&&(g==0))&&(d==13))&&(f==-8))&&(e==5)");
	}

	@Test
	public void test11() {
		// a = b
		// c = 3 + a
		// a > 3
		IntVariable v1 = new IntVariable("a", 0, 99);
		IntVariable v2 = new IntVariable("b", 0, 99);
		IntVariable v3 = new IntVariable("c", 0, 99);
		IntConstant c1 = new IntConstant(3);
		Operation o1 = new Operation(Operation.Operator.EQ, v1, v2);

		Operation o2 = new Operation(Operation.Operator.ADD, c1, v1);
		Operation o3 = new Operation(Operation.Operator.EQ, v3, o2);

		Operation o4 = new Operation(Operation.Operator.AND, o1, o3);
		Operation o5 = new Operation(Operation.Operator.GT, v1, c1);
		Operation o6 = new Operation(Operation.Operator.AND, o4, o5);
		System.out.println("operation = " + o6);
		check(o6, "((c==(3+b))&&(b>3))&&(a==b)");
	}

	@Test
	public void test12() {
		// a = b
		// b = 5
		// c = 3 + a
		IntVariable v1 = new IntVariable("a", 0, 99);
		IntVariable v2 = new IntVariable("b", 0, 99);
		IntVariable v3 = new IntVariable("c", 0, 99);

		IntConstant c1 = new IntConstant(3);
		IntConstant c2 = new IntConstant(5);

		Operation eq1 = new Operation(Operation.Operator.EQ, v1, v2);
		Operation eq2 = new Operation(Operation.Operator.EQ, v2, c2);

		Operation o1 = new Operation(Operation.Operator.ADD, c1, v1);
		Operation eq3 = new Operation(Operation.Operator.EQ, v3, o1);


		Operation phi1 = new Operation(Operation.Operator.AND, eq1, eq2);
		Operation phi2 = new Operation(Operation.Operator.AND, phi1, eq3);

		System.out.println("operation = " + phi2);
		check(phi2, "((a==b)&&(b==5))&&(c==8)");
	}
}
