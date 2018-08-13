package za.ac.sun.cs.green.service.simplify;



import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

public class ConstantPropagation extends BasicService {
	private int invocations = 0;
	static Map<Expression, Expression> map = new HashMap<>();
	private static Map<Expression, Expression> specialMap = new HashMap<>();
	static boolean changed = false;
	static boolean unsatisfiable = false;
	static int ONE = 1;
	public static void main (String[] args) {
		Operation operation = test11();
		System.out.println(operation);
		constantVisitor ov = new constantVisitor();
		try {
			operation.accept(ov);
			while (changed) {
				changed = false;
				ov.getExpression().accept(ov);
				if (changed == false && ONE == 1) {
					changed = true;
					ONE--;
				}
			}
			ov.print();
		} catch (VisitorException e) {
			e.printStackTrace();
		}
	}

	public static Operation test00() {
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
		//check(o4, "(x==1)&&(y==9)");
		return o4;
	}


	public static Operation test01() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(1);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.LT, o2, c2); // o3 : (x+y) < 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	public static Operation test02() {
		IntConstant c1 = new IntConstant(4);
		IntConstant c2 = new IntConstant(10);
		Operation o = new Operation(Operation.Operator.LT, c1, c2);
		//check(o, "0==0");
		return o;
	}


	public static Operation test03() {
		IntConstant c1 = new IntConstant(4);
		IntConstant c2 = new IntConstant(10);
		Operation o = new Operation(Operation.Operator.GT, c1, c2);
		//check(o, "0==1");
		return o;
	}


	public static Operation test04() {
		IntConstant c1 = new IntConstant(4);
		IntConstant c2 = new IntConstant(10);
		Operation o1 = new Operation(Operation.Operator.LT, c1, c2);
		Operation o2 = new Operation(Operation.Operator.GT, c1, c2);
		Operation o = new Operation(Operation.Operator.AND, o1, o2);
		//check(o, "0==1");
		return o;
	}

	public static Operation test05() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);		
		IntConstant c = new IntConstant(1);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, c, x);
		Operation o2 = new Operation(Operation.Operator.ADD, x, y);
		Operation o3 = new Operation(Operation.Operator.LT, o2, c2);
		Operation oi = new Operation(Operation.Operator.SUB, y, c);		
		Operation o4 = new Operation(Operation.Operator.EQ, c3, oi);
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3);
		Operation o = new Operation(Operation.Operator.AND, o5, o4);
		//check(o, "(1==x)&&(3==y)");
		return o;
	}

	public static Operation test06() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0 , 99);
		IntConstant c = new IntConstant(1);
		Operation o1 = new Operation(Operation.Operator.EQ, x, y);		
		Operation o2 = new Operation(Operation.Operator.EQ, y, z);
		Operation o3 = new Operation(Operation.Operator.EQ, z, c);
		Operation o = new Operation(Operation.Operator.AND, o1, o2);
		o = new Operation(Operation.Operator.AND, o, o3);
		//check(o, "(x==1)&&((y==1)&&(z==1))");
		return o;
	}


	public static Operation test07() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntVariable z = new IntVariable("z", 0 , 99);
		IntConstant c = new IntConstant(2);
		IntConstant c1 = new IntConstant(4);
		Operation o1 = new Operation(Operation.Operator.MUL, x, y);		
		Operation o2 = new Operation(Operation.Operator.EQ, z, o1); // z = x * y
		Operation o3 = new Operation(Operation.Operator.EQ, x, c); // x = 2
		Operation o4 = new Operation(Operation.Operator.ADD, y, x); 
		Operation o5 = new Operation(Operation.Operator.EQ, o4, c1); // x+y = 4

		Operation o = new Operation(Operation.Operator.AND, o2, o3); // z = x * y && x = 2
		o = new Operation(Operation.Operator.AND, o, o5); // z = x * y && x = 2 && x+y = 4
		//check(o, "(z==4)&&((x==2)&&(y==2))");
		return o;
	}


	public static Operation test08() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntConstant c = new IntConstant(2);
		IntConstant c1 = new IntConstant(4);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c);		
		Operation o2 = new Operation(Operation.Operator.EQ, x, c1);
		Operation o = new Operation(Operation.Operator.AND, o1, o2);
		return o;
		//check(o, "0==1");
	}

	public static Operation test09() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(1);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.GT, o2, c2); // o3 : (x+y) < 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	public static Operation test10() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(1);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.LT, o2, c2); // o3 : (x+y) < 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	public static Operation test11() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(1);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.ADD, c2, x); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.LT, y, o2); // o3 : (x+y) < 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (y < 10 + x) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	public static Operation test12() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(6);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(3);
		IntConstant c1 = new IntConstant(1);
		IntConstant c4 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.LE, o2, c2); // o3 : (x+y) <= 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c1); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	public static Operation test13() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(6);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(3);
		IntConstant c1 = new IntConstant(1);
		IntConstant c4 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.GT, o2, c2); // o3 : (x+y) <= 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c1); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	public static Operation test14() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(6);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(3);
		IntConstant c1 = new IntConstant(1);
		IntConstant c4 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.MUL, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.GT, o2, c2); // o3 : (x+y) <= 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c1); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	public static Operation test15() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(6);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(3);
		IntConstant c1 = new IntConstant(1);
		IntConstant c4 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.MUL, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.NE, o2, c2); // o3 : (x+y) <= 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c1); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	public static Operation test16() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(6);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(3);
		IntConstant c1 = new IntConstant(1);
		IntConstant c4 = new IntConstant(2);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.DIV, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.NE, o2, c2); // o3 : (x+y) <= 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c1); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		return o;
	}

	/*
	 * TODO
	 */
	public ConstantPropagation(Green solver) {
		super(solver);
	}

	/*
	 * TODO
	 */
	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	/*
	 * TODO
	 */
	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Expression e = propagate(instance.getFullExpression());
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}
	
	/*
	 * TODO
	 */
	public Expression propagate(Expression expression) {
		try {
			specialMap = new HashMap<>();
			ONE = 1;
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			constantVisitor constantVisitor = new constantVisitor();
			expression.accept(constantVisitor);
			while (changed) {
				changed = false;
				constantVisitor.getExpression().accept(constantVisitor);
				//One extra iteration to account for expressions like 3 < 5 after variable propagation
				if (changed == false && ONE == 1) {
					ONE--;
					changed = true;
				}
			}
			Expression canonized = constantVisitor.getExpression();
			log.log(Level.FINEST, "After Canonization: " + canonized);
			return canonized;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	/*
	 * TODO
	 */
	static class constantVisitor extends Visitor {

		private Stack<Expression> stack;

		public constantVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		/*
		 * TODO
		 */
		@Override
		public void preVisit(Operation operation) throws VisitorException {
			//Checks if current expression is equality check
			if (operation.getOperator().equals(Operation.Operator.EQ)) {
				//Variable to constant
				if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntConstant) {
					if (specialMap.containsKey(operation.getOperand(0)) && !(specialMap.get(operation.getOperand(0)).equals(operation.getOperand(1)))) {
						if (specialMap.get(operation.getOperand(0)) instanceof IntConstant) {
							unsatisfiable = true;
						} else {
							specialMap.replace(operation.getOperand(0), operation.getOperand(1));
							changed = true;
						}	
					} else if (!specialMap.containsKey(operation.getOperand(0))) {
						changed = true;
						specialMap.put(operation.getOperand(0), operation.getOperand(1));
					}
				//Constant to variable
				} else if (operation.getOperand(0) instanceof IntConstant && operation.getOperand(1) instanceof IntVariable) {
					if (specialMap.containsKey(operation.getOperand(1)) && !(specialMap.get(operation.getOperand(1)).equals(operation.getOperand(0)))){
						specialMap.replace(operation.getOperand(1), operation.getOperand(0));
						changed = true;
					} else if (!specialMap.containsKey(operation.getOperand(1))) {
						changed = true;
						specialMap.put(operation.getOperand(1), operation.getOperand(0));
					}
				//Variable to variable
				} else if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntVariable) {
					if (specialMap.containsKey(operation.getOperand(0)) && !(specialMap.get(operation.getOperand(0)).equals(operation.getOperand(1)))) {
						specialMap.replace(operation.getOperand(0), operation.getOperand(1));
						changed = true;
					} else if (specialMap.containsKey(operation.getOperand(1)) && !(specialMap.get(operation.getOperand(1)).equals(operation.getOperand(0)))) {
						specialMap.replace(operation.getOperand(1), operation.getOperand(0));
						changed = true;
					} else {
						if (!specialMap.containsKey(operation.getOperand(0))) {
							changed = true;
							specialMap.put(operation.getOperand(0), operation.getOperand(1));
						} else if (!specialMap.containsKey(operation.getOperand(1))) {
							changed = true;
							specialMap.put(operation.getOperand(1), operation.getOperand(0));
						}
					} 
				}
			}
			if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntVariable) {
				Object t[] = specialMap.keySet().toArray();
				for (int i = t.length-1; i >= 0; i--) {
					if (specialMap.containsKey(specialMap.get(t[i]))) {
						specialMap.replace((IntVariable) t[i], specialMap.get(specialMap.get(t[i])));
					}
				}	
			}	
		}

		@Override
		public void postVisit(IntConstant constant) throws VisitorException {
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
		}
		
		public void print() {
			System.out.println(stack);
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();
			Operation.Operator nop = null;
			switch (op) {
			case EQ:
				nop = Operation.Operator.EQ;
				break;
			case NE:
				nop = Operation.Operator.NE;
				break;
			case LT:
				nop = Operation.Operator.GT;
				break;
			case LE:
				nop = Operation.Operator.GE;
				break;
			case GT:
				nop = Operation.Operator.LT;
				break;
			case GE:
				nop = Operation.Operator.LE;
				break;
			default:
				break;
			}
			if (nop != null) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				IntConstant constant = null;
				IntVariable variable = null;
				Operation oper = null;
				//Equate function
				if (nop.equals(Operation.Operator.EQ)) {
					//left side is an operation, e.g. 2 + 2, 2 + x, x + y 
					if (l instanceof Operation) {
						//right side contains a variable
						if (r instanceof IntVariable) {
							//Replace variable with value if it exists in the map
							if (specialMap.containsKey(r)) {
								variable = (IntVariable) specialMap.get(r);
							} else {
								variable = (IntVariable) r;
							}
							//If both values on the left side are constants
							if (((Operation) l).getOperand(0) instanceof IntConstant && ((Operation) l).getOperand(1) instanceof IntConstant) {
								//Simplify the constant values
								if (((Operation) l).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() + ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() - ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() * ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() / ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								}
							} 
							//Simplified peration to push to the stack
							oper = new Operation(Operation.Operator.EQ, constant, variable);
						} else if (r instanceof IntConstant) {
							int coeff = 1;
							if (((Operation) l).getOperand(0) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) l).getOperand(1);
								if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								}
								constant = new IntConstant(coeff*(((IntConstant) r).getValue() - ((IntConstant) ((Operation) l).getOperand(0)).getValue()));
							} else if (((Operation) l).getOperand(1) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) l).getOperand(0);
								if (((Operation) l).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant) r).getValue() - ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant) r).getValue() + ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() * ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() / ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								}
							}
							oper = new Operation(Operation.Operator.EQ, variable, constant);
						}
						stack.push(oper);
					} else if (r instanceof Operation) {
						if (l instanceof IntVariable) {
							variable = (IntVariable) l;
							if (((Operation) r).getOperand(0) instanceof IntConstant && ((Operation) r).getOperand(1) instanceof IntConstant) {
								if (((Operation) r).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant) ((Operation) r).getOperand(0)).getValue() + ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant) ((Operation) r).getOperand(0)).getValue() - ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(((IntConstant) ((Operation) r).getOperand(0)).getValue() * ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(((IntConstant) ((Operation) r).getOperand(0)).getValue() / ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								}
								oper = new Operation(Operation.Operator.EQ, l, constant);
							} else {
								oper = new Operation(Operation.Operator.EQ, l, r);
							}
						} else if (l instanceof IntConstant) {
							if (((Operation) r).getOperand(0) instanceof IntConstant) {
								int coeff = 1;
								if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								}
								variable = (IntVariable) ((Operation) r).getOperand(1);
								constant = new IntConstant(coeff*(((IntConstant) l).getValue() - ((IntConstant) ((Operation) r).getOperand(0)).getValue()));
								oper = new Operation(Operation.Operator.EQ, constant, variable);
							} else if (((Operation) r).getOperand(1) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) r).getOperand(0);
								if (((Operation) r).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant) l).getValue() - ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant) l).getValue() + ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} 
								oper = new Operation(Operation.Operator.EQ, constant, variable);
							} else {
								oper = operation;
							}
						}
						stack.push(oper);
					} else if (l instanceof IntConstant && r instanceof IntConstant) {
						if (op.equals(Operation.Operator.ADD)) {
							constant = new IntConstant(((IntConstant)(l)).getValue() + ((IntConstant)(r)).getValue());
						} else if (op.equals(Operation.Operator.SUB)) {
							constant = new IntConstant(((IntConstant)(l)).getValue() - ((IntConstant)(r)).getValue());
						} else {
							stack.push(operation);
						}
					} else {
						if (specialMap.containsKey(l) && l instanceof IntVariable) {
							if (!specialMap.get(l).equals(r) && r instanceof IntConstant) {
								stack.push(Operation.FALSE);
							} else if (l instanceof IntVariable) {
								stack.push(new Operation(nop, l, specialMap.get(l)));	
							} 
						} else if (specialMap.containsKey(l) && l instanceof IntVariable) {
							if (!specialMap.get(r).equals(l) && l instanceof IntConstant) {
								stack.push(Operation.FALSE);
							} else if (r instanceof IntVariable) {
								stack.push(new Operation(nop, r, specialMap.get(r)));	
							} 
						} else {
							stack.push(new Operation(nop, l, r));
						}
					}

				} else if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(nop, specialMap.get(l), specialMap.get(r)));
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(nop, l, specialMap.get(r)));
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(nop, specialMap.get(l), r));
					} else {
						stack.push(new Operation(nop, l, r));
					}
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), specialMap.get(r)));
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(op, l, specialMap.get(r)));
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), r));
					} else {
						stack.push(new Operation(op, l, r));
					}
				} else if ((l instanceof IntVariable)
						&& (r instanceof IntConstant)) {
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), specialMap.get(r)));
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(op, l, specialMap.get(r)));
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), r));
					} else {
						stack.push(new Operation(op, l, r));
					}
				} else {
					if (l instanceof IntConstant && r instanceof IntConstant) {
						switch (op) {
						case GE:
							if (((IntConstant)l).getValue() >= ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						case GT:
							if (((IntConstant)l).getValue() > ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						case LE:
							if (((IntConstant)l).getValue() <= ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						case LT:
							if (((IntConstant)l).getValue() < ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						case NE:
							if (((IntConstant)l).getValue() != ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						default:
							break;
						}
					} else if (l instanceof Operation) {
						if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntConstant) {
							switch (((Operation) l).getOperator()) {
							case ADD:
								constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() + ((IntConstant)((Operation)l).getOperand(1)).getValue());
								if (specialMap.containsKey(r)) {
									oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
								} else {
									oper = new Operation(operation.getOperator(), constant, r);
								}
								break;
							case SUB:
								constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() - ((IntConstant)((Operation)l).getOperand(1)).getValue());
								oper = new Operation(operation.getOperator(), constant, r);
								break;
							case MUL:
								constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() * ((IntConstant)((Operation)l).getOperand(1)).getValue());
								oper = new Operation(operation.getOperator(), constant, r);
								break;
							case DIV:
								constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() / ((IntConstant)((Operation)l).getOperand(1)).getValue());
								oper = new Operation(operation.getOperator(), constant, r);
								break;
							default:
								oper = operation;
								break;
							}
							if (oper.getOperand(0) instanceof IntConstant && oper.getOperand(1) instanceof IntConstant) {
								switch (oper.getOperator()) {
								case GE:
									if (((IntConstant)oper.getOperand(0)).getValue() >= ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								case GT:
									if (((IntConstant)oper.getOperand(0)).getValue() > ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								case LE:
									if (((IntConstant)oper.getOperand(0)).getValue() <= ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								case LT:
									if (((IntConstant)oper.getOperand(0)).getValue() < ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								case NE:
									if (((IntConstant)oper.getOperand(0)).getValue() != ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								default:
									break;
								}
							}
						} else if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntVariable) {
							int coeff = 1;
							if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
								coeff = -1;
							} 
							if (l instanceof IntConstant) {
								constant = new IntConstant(coeff*(((IntConstant)r).getValue() - ((IntConstant)((Operation)l).getOperand(0)).getValue()));	
								oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
							} else {
								oper = operation;
							}
						} else {
							oper = operation;
						}
					} else if (r instanceof Operation) {
						if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntConstant) {
							switch (((Operation) r).getOperator()) {
							case ADD:
								constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() + ((IntConstant)((Operation)r).getOperand(1)).getValue());
								oper = new Operation(operation.getOperator(), l, constant);
								break;
							case SUB:
								constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
								oper = new Operation(operation.getOperator(), l, constant);
								break;
							case MUL:
								constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() * ((IntConstant)((Operation)r).getOperand(1)).getValue());
								oper = new Operation(operation.getOperator(), l, constant);
								break;
							case DIV:
								constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() / ((IntConstant)((Operation)r).getOperand(1)).getValue());
								oper = new Operation(operation.getOperator(), l, constant);
								break;
							default:
								oper = operation;
								break;
							}
						} else if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntVariable) {
							int coeff = 1;
							if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
								coeff = -1;
							} 
							if (l instanceof IntConstant) {
								constant = new IntConstant(coeff*(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(0)).getValue()));					
								oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
							} else {
								oper = operation;
							}	
						} else {
							oper = (Operation) Operation.TRUE;
						}
					} 
					stack.push(oper);
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				Expression shrink = null;
				if (r.equals(Operation.TRUE) || r.equals(Operation.FALSE) || l.equals(Operation.TRUE) || l.equals(Operation.FALSE)) {

					if (r.equals(Operation.TRUE)) {
						shrink = l;
					} else if (r.equals(Operation.FALSE)) {
						shrink = r;
					} 
					if (l.equals(Operation.TRUE)) {
						shrink = r;
					} else if (l.equals(Operation.FALSE)) {
						shrink = l;
					}
					stack.push(shrink);
				} else {
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), specialMap.get(r)));
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(op, l, specialMap.get(r)));
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l) , r));
					} else {
						stack.push(new Operation(op, l, r));
					} 
				}
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}
	}
}
