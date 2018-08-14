package za.ac.sun.cs.green.service.simplify;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Test;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.util.Configuration;

public class OnlyConstantPropagationTest {

	public static Green solver ;

	@BeforeClass
		public static void initialize() {
			solver = new Green();
			Properties props = new Properties();
			props.setProperty("green.services", "sat");
			props.setProperty("green.service.sat", "(simplify sink)");
			/*props.setProperty("green.service.sat", "(canonize sink)");*/
			props.setProperty("green.service.sat.simplify",
					"za.ac.sun.cs.green.service.simplify.ConstantPropagation");
			/*props.setProperty("green.service.sat.canonize",
				"za.ac.sun.cs.green.service.canonizer.SATCanonizerService");*/
			props.setProperty("green.service.sat.sink",
					"za.ac.sun.cs.green.service.sink.SinkService");
			Configuration config = new Configuration(solver, props);
			config.configure();
		}

	private void finalCheck(String observed, String expected) {
		assertEquals(expected, observed);
	}

	private void check(Expression expression, String expected) {
		Instance i = new Instance(solver, null, null, expression);
		Expression e = i.getExpression();
		assertTrue(e.equals(expression));
		assertEquals(expression.toString(), e.toString());
		Object result = i.request("sat");
		assertNotNull(result);
		assertEquals(Instance.class, result.getClass());
		Instance j = (Instance) result;
		finalCheck(j.getExpression().toString(), expected);
	}

	@Test
	public void test00() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0, 99);
		IntConstant c = new IntConstant(1);
		IntConstant c10 = new IntConstant(10);
		IntConstant c3 = new IntConstant(3);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : x = 1
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : (x + y)
		Operation o3 = new Operation(Operation.Operator.EQ, o2, c10); // o3 : x+y = 10
		Operation o4 = new Operation(Operation.Operator.AND, o1, o3); // o4 : x = 1 && (x+y) = 10
		check(o4, "(x==1)&&((1+y)==10)");
	}

  @Test
	public void test01() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0, 99);
		IntConstant c = new IntConstant(1);
		IntConstant c10 = new IntConstant(10);
		IntConstant c3 = new IntConstant(3);
		Operation o1 = new Operation(Operation.Operator.EQ, c, x); // o1 : x = 1
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : (x + y)
		Operation o3 = new Operation(Operation.Operator.EQ, o2, c10); // o3 : x+y = 10
		Operation o4 = new Operation(Operation.Operator.AND, o1, o3); // o4 : x = 1 && (x+y) = 10
		check(o4, "(x==1)&&((1+y)==10)");
	}

   @Test
	public void test02() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0, 99);
		IntConstant c = new IntConstant(1);
		IntConstant c10 = new IntConstant(10);
		IntConstant c3 = new IntConstant(3);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : x = 1
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : (x + y)
		Operation o3 = new Operation(Operation.Operator.EQ, o2, c10); // o3 : x+y = 10
		Operation o4 = new Operation(Operation.Operator.AND, o3, o1); // o4 : x = 1 && (x+y) = 10
		check(o4, "((1+y)==10)&&(x==1)");
	}


	@Test
	public void test03() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0, 99);
		IntConstant c = new IntConstant(1);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x == 1)
		Operation o2 = new Operation(Operation.Operator.EQ, x, y); // o2 : (x == y)
		Operation o3 = new Operation(Operation.Operator.EQ, y, z); // o3 : (y == z)
		Operation o4 = new Operation(Operation.Operator.AND, o1, o2); // o4 :(x==1) && (x==y)
    Operation o5 = new Operation(Operation.Operator.AND, o4, o3); // o4 :(x==1) && (x==y) && (y == z)
		check(o5, "((x==1)&&(y==1))&&(z==1)");
	}

  @Test
	public void test04() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0, 99);
		IntConstant c = new IntConstant(1);
		Operation o1 = new Operation(Operation.Operator.EQ, z, y); // o1 : (z == y)
		Operation o2 = new Operation(Operation.Operator.EQ, y, x); // o2 : (y == x)
		Operation o3 = new Operation(Operation.Operator.EQ, x, c); // o3 : (x == 1)
		Operation o4 = new Operation(Operation.Operator.AND, o1, o2); // o4 :(z==y) && (y==x)
    Operation o5 = new Operation(Operation.Operator.AND, o4, o3); // o4 :(z==y) && (y==x) && (x == 1)
		check(o5, "((z==1)&&(y==1))&&(x==1)");
	}

  @Test
	public void test05() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0, 99);
		IntConstant c = new IntConstant(1);
		Operation o1 = new Operation(Operation.Operator.EQ, y, z); // o1 : (y == z)
		Operation o2 = new Operation(Operation.Operator.EQ, x, y); // o2 : (x == y)
		Operation o3 = new Operation(Operation.Operator.EQ, c, x); // o3 : (1 == x)
		Operation o4 = new Operation(Operation.Operator.AND, o1, o2); // o4 :(z==y) && (y==x)
    Operation o5 = new Operation(Operation.Operator.AND, o4, o3); // o4 :(z==y) && (y==x) && (x == 1)
    check(o5, "((z==1)&&(y==1))&&(x==1)");
	}

	@Test
	public void test06() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0, 99);
		IntConstant c = new IntConstant(1);
		Operation o1 = new Operation(Operation.Operator.EQ, c, x); // o1 : (1 == x)
		Operation o2 = new Operation(Operation.Operator.EQ, x, y); // o2 : (x == y)
		Operation o3 = new Operation(Operation.Operator.EQ, y, z); // o3 : (y == z)
		Operation o4 = new Operation(Operation.Operator.AND, o1, o2); // o4 :(1==x) && (x==y)
    Operation o5 = new Operation(Operation.Operator.AND, o4, o3); // o4 :(1==x) && (x==y) && (y == z)
  	check(o5, "((x==1)&&(y==1))&&(z==1)");
	}
}
