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
	static int ONE_EXTRA = 1;
	public static void main (String[] args) {

		Operation operation = test13();
		System.out.println(operation);
		constantVisitor ov = new constantVisitor();
		try {
			operation.accept(ov);
			ov.print();
			while (changed) {
				changed = false;
				ov.getExpression().accept(ov);
				if (changed == false && ONE_EXTRA == 1) {
					ov.getExpression().accept(ov);
					ONE_EXTRA--;
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
	
	public ConstantPropagation(Green solver) {
		super(solver);
	}
	
	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}
	
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
	
	public Expression propagate(Expression expression) {
		try {
			specialMap = new HashMap<>();
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			constantVisitor constantVisitor = new constantVisitor();
			expression.accept(constantVisitor);
			while (changed) {
				changed = false;
				constantVisitor.getExpression().accept(constantVisitor);
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
	
	static class constantVisitor extends Visitor {
		
		private Stack<Expression> stack;
		
		public constantVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) throws VisitorException {
			//System.out.println("Constant: " + constant);
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			//System.out.println("Variable: " + variable);
			stack.push(variable);
		}
		
		@Override
		public void preVisit(Operation operation) throws VisitorException {
			if (operation.getOperator().equals(Operation.Operator.EQ)) {
				System.out.println("Previsit");
				if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntConstant) {
					System.out.println("PRE VISIT 1 " + operation);
					System.out.println(operation.getOperand(0) + " " + specialMap.get(operation.getOperand(0)) + " " + (operation.getOperand(1)));
					if (specialMap.containsKey(operation.getOperand(0)) && !(specialMap.get(operation.getOperand(0)).equals(operation.getOperand(1)))) {
						if (specialMap.get(operation.getOperand(0)) instanceof IntConstant) {
							System.out.println("Invalid move");
							unsatisfiable = true;
						} else {
							specialMap.replace(operation.getOperand(0), operation.getOperand(1));
							System.out.println("Some cooolio 1 ");
							changed = true;
							System.out.println("CHANGED 1");
						}
						
					} else if (!specialMap.containsKey(operation.getOperand(0))) {
						changed = true;
						System.out.println("CHANGED 2");
						specialMap.put(operation.getOperand(0), operation.getOperand(1));
						System.out.println(specialMap);
					}
					//specialMap.put(operation.getOperand(0), operation.getOperand(1));
					
					
					//changed = true;
				} else if (operation.getOperand(0) instanceof IntConstant && operation.getOperand(1) instanceof IntVariable) {
					System.out.println("PRE VISIT 2 " + operation);
					System.out.println(operation.getOperand(1) + " " + specialMap.get(operation.getOperand(1)) + " " + (operation.getOperand(0)));
					if (specialMap.containsKey(operation.getOperand(1)) && !(specialMap.get(operation.getOperand(1)).equals(operation.getOperand(0)))){
						specialMap.replace(operation.getOperand(1), operation.getOperand(0));
						System.out.println("Some cooolio 1 ");
						System.out.println("CHANGED 3");
						changed = true;
						System.out.println(specialMap);
					} else if (!specialMap.containsKey(operation.getOperand(1))) {
						changed = true;
						System.out.println("CHANGED 4");
						specialMap.put(operation.getOperand(1), operation.getOperand(0));
						System.out.println(specialMap);
						System.out.println("WHAT WHAT WHAT");
					}
					//specialMap.put(operation.getOperand(1), operation.getOperand(0));
					//changed = true;
				} else if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntVariable) {
					if (specialMap.containsKey(operation.getOperand(0)) && !(specialMap.get(operation.getOperand(0)).equals(operation.getOperand(1)))) {
						specialMap.replace(operation.getOperand(0), operation.getOperand(1));
						System.out.println("Some cooolio 1 ");
						changed = true;
						System.out.println("CHANGED 5");
						System.out.println(specialMap);
					} else if (specialMap.containsKey(operation.getOperand(1)) && !(specialMap.get(operation.getOperand(1)).equals(operation.getOperand(0)))) {
						specialMap.replace(operation.getOperand(1), operation.getOperand(0));
						System.out.println("Some cooolio 2 ");
						changed = true;
						System.out.println("CHANGED 5");
						System.out.println(specialMap);
					} else {
						if (!specialMap.containsKey(operation.getOperand(0))) {
							changed = true;
							System.out.println("CHANGED 6");
							specialMap.put(operation.getOperand(0), operation.getOperand(1));
							System.out.println(specialMap);
						} else if (!specialMap.containsKey(operation.getOperand(1))) {
							changed = true;
							System.out.println("CHANGED 7");
							specialMap.put(operation.getOperand(1), operation.getOperand(0));
							System.out.println(specialMap);
						}
					} 
					
					//specialMap.put(operation.getOperand(0), operation.getOperand(1));
					//changed = true;
				}

				
			}
			if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntVariable) {
				System.out.println("OPER : " + operation);
				System.out.println(specialMap + " SM");
				Object t[] = specialMap.keySet().toArray();
			
				for (int i = t.length-1; i >= 0; i--) {
					//System.out.println(specialMap.get(specialMap.keySet()));
					if (specialMap.containsKey(specialMap.get(t[i]))) {
						System.out.println(t[i] + " ---> " + specialMap.get(specialMap.get(t[i])));
						specialMap.replace((IntVariable) t[i], specialMap.get(specialMap.get(t[i])));
					}
				}
				
			}
			
		}
		
		public void print() {
			System.out.println("Special map " + specialMap);
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
				System.out.println(op + " OPERATOR" + " l : " + l + " r : " + r);
				if (nop.equals(Operation.Operator.EQ)) {
					System.out.println("Operation in NOP: " + operation);
					if (l instanceof Operation) {
						if (r instanceof IntVariable) {
							if (specialMap.containsKey(r)) {
								variable = (IntVariable) specialMap.get(r);
							} else {
								variable = (IntVariable) r;
							}
							System.out.println("1");
							if (((Operation) l).getOperand(0) instanceof IntConstant && ((Operation) l).getOperand(1) instanceof IntConstant) {
								//Check operator
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
							oper = new Operation(Operation.Operator.EQ, constant, variable);
						} else if (r instanceof IntConstant) {
							System.out.println("2");
							int coeff = 1;
							
							if (((Operation) l).getOperand(0) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) l).getOperand(1);
								if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									System.out.println("Negative coeff");
									coeff = -1;
								}
								
								System.out.println("Left is constant " + ((Operation) l).getOperator());
								constant = new IntConstant(coeff*(((IntConstant) r).getValue() - ((IntConstant) ((Operation) l).getOperand(0)).getValue()));
								
							} else if (((Operation) l).getOperand(1) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) l).getOperand(0);
								System.out.println("Right is constant " + ((Operation) l).getOperator());
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
							System.out.println("PUSHED");
							
						}
						stack.push(oper);
					} else if (r instanceof Operation) {
						if (l instanceof IntVariable) {
							System.out.println("3");
							variable = (IntVariable) l;
							if (((Operation) r).getOperand(0) instanceof IntConstant && ((Operation) r).getOperand(1) instanceof IntConstant) {
								System.out.println(" " + ((Operation) r).getOperand(0));
								System.out.println(" " + ((Operation) r).getOperand(1));
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
							System.out.println(l + " " + r);
							System.out.println("4");
							System.out.println(((Operation) r).getOperand(0) + " AKSJDAS " + ((Operation) r).getOperand(1));
						
							
							if (((Operation) r).getOperand(0) instanceof IntConstant) {
								int coeff = 1;
								if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									System.out.println("Negative coeff");
									coeff = -1;
								}
								variable = (IntVariable) ((Operation) r).getOperand(1);
								System.out.println("Left is constant " + ((Operation) r).getOperator());
								constant = new IntConstant(coeff*(((IntConstant) l).getValue() - ((IntConstant) ((Operation) r).getOperand(0)).getValue()));
								oper = new Operation(Operation.Operator.EQ, constant, variable);
							} else if (((Operation) r).getOperand(1) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) r).getOperand(0);
								System.out.println("Right is constant " + ((Operation) r).getOperator());
								if (((Operation) r).getOperator().equals(Operation.Operator.ADD)) {
									System.out.println("HELP");
									constant = new IntConstant(((IntConstant) l).getValue() - ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									System.out.println("HELP");
									constant = new IntConstant(((IntConstant) l).getValue() + ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								}
								oper = new Operation(Operation.Operator.EQ, constant, variable);
							} else {
								System.out.println(operation + " OPER");
								oper = operation;
							}
						}
						stack.push(oper);
					} else if (l instanceof IntConstant && r instanceof IntConstant) {
						//Check function
						
						if (op.equals(Operation.Operator.ADD)) {
							System.out.println("USELESS 1");
							constant = new IntConstant(((IntConstant)(l)).getValue() + ((IntConstant)(r)).getValue());
						} else if (op.equals(Operation.Operator.SUB)) {
							System.out.println("USELESS 2");
							constant = new IntConstant(((IntConstant)(l)).getValue() - ((IntConstant)(r)).getValue());
						} else {
							stack.push(operation);
						}
					} else {
						System.out.println("BIG ELSE");
						if (specialMap.containsKey(l) && l instanceof IntVariable) {
							System.out.println("BE 1");
							if (!specialMap.get(l).equals(r) && r instanceof IntConstant) {
								System.out.println("MY DOODE");
								System.out.println(specialMap.get(l) + " | " + r);
								stack.push(Operation.FALSE);
							} else {
								stack.push(new Operation(nop, l, specialMap.get(l)));
							}
						} else if (specialMap.containsKey(r) && r instanceof IntVariable){
							stack.push(new Operation(nop, r, specialMap.get(r)));
						} else {
							stack.push(new Operation(nop, l, r));
							System.out.println("BE 3");
						}
						System.out.println(stack);
					}
					System.out.println("Constant: " + constant + "\nVariable: " + variable + "\nOperation: " + oper);
					
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
					System.out.println("he 1");
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
					System.out.println("he 2");
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
					System.out.println("he 3");
				} else {
					System.out.println("Other operation: " + op);
					switch (op) {
					case GE:
						System.out.println("Greater than/ equal");
						System.out.println(l + " " + r + " tag");
						if (l instanceof IntConstant && r instanceof IntConstant) {
							if (((IntConstant)l).getValue() >= ((IntConstant)r).getValue()) {
								System.out.println("ITS TRUE");
								oper = (Operation) Operation.TRUE;
							} else {
								System.out.println("FALSE");
								oper = (Operation) Operation.FALSE;
							}
						} else if (l instanceof Operation) {
							if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntConstant) {
								switch (((Operation) l).getOperator()) {
								case ADD:
									System.out.println("add");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() + ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									if (specialMap.containsKey(r)) {
										oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
									} else {
										oper = new Operation(operation.getOperator(), constant, r);
									}
									
									break;
								case SUB:
									System.out.println("sub");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() - ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								case MUL:
									System.out.println("mul");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() * ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								case DIV:
									System.out.println("div");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() / ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								default:
									oper = operation;
									break;
								}
							} else if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntVariable) {
								System.out.println(operation.getOperator() + " " + r);
								System.out.println("Case 1");
								int coeff = 1;
								if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								} 
								if (l instanceof IntConstant) {
									System.out.println("$5");
									constant = new IntConstant(coeff*(((IntConstant)r).getValue() - ((IntConstant)((Operation)l).getOperand(0)).getValue()));
									
									oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
								} else {
									//constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									oper = operation;
								}
							} else if (((Operation)l).getOperand(1) instanceof IntConstant && ((Operation)l).getOperand(0) instanceof IntVariable) {
								System.out.println("Case 2");
							} else {
								System.out.println("Case 3: ");
								//oper = operation;
								oper = (Operation) Operation.TRUE;
							}
						} else if (r instanceof Operation) {
							if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntConstant) {
								System.out.println(r + " RIGHT123");
								switch (((Operation) r).getOperator()) {
								case ADD:
									System.out.println("add");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() + ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case SUB:
									System.out.println("sub");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case MUL:
									System.out.println("mul");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() * ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case DIV:
									System.out.println("div");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() / ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								default:
									oper = operation;
									break;
								}
							} else if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntVariable) {
								System.out.println(operation.getOperator() + " " + r);
								System.out.println("Case 1");
								int coeff = 1;
								if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								} 
								if (l instanceof IntConstant) {
									System.out.println("$5");
									constant = new IntConstant(coeff*(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(0)).getValue()));
									
									oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
								} else {
									//constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									oper = operation;
								}
								
								//oper = new Operation(operation.getOperator(), )
							} else if (((Operation)r).getOperand(1) instanceof IntConstant && ((Operation)r).getOperand(0) instanceof IntVariable) {
								System.out.println("Case 2");
							} else {
								System.out.println("Case 3: ");
								//oper = operation;
								oper = (Operation) Operation.TRUE;
							}
						} else if (r instanceof IntConstant) {
							System.out.println("12345");
						} else if (l instanceof IntConstant) {
							System.out.println("123456");
						}
						break;
					case GT:
						System.out.println("Greater than");
						System.out.println(l + " " + r + " tag");
						if (l instanceof IntConstant && r instanceof IntConstant) {
							if (((IntConstant)l).getValue() > ((IntConstant)r).getValue()) {
								System.out.println("ITS TRUE");
								oper = (Operation) Operation.TRUE;
							} else {
								System.out.println("FALSE");
								oper = (Operation) Operation.FALSE;
							}
						} else if (l instanceof Operation) {
							if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntConstant) {
								switch (((Operation) l).getOperator()) {
								case ADD:
									System.out.println("add");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() + ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									if (specialMap.containsKey(r)) {
										oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
									} else {
										oper = new Operation(operation.getOperator(), constant, r);
									}
									
									break;
								case SUB:
									System.out.println("sub");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() - ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								case MUL:
									System.out.println("mul");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() * ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								case DIV:
									System.out.println("div");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() / ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								default:
									oper = operation;
									break;
								}
							} else if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntVariable) {
								System.out.println(operation.getOperator() + " " + r);
								System.out.println("Case 1");
								int coeff = 1;
								if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								} 
								if (l instanceof IntConstant) {
									System.out.println("$5");
									constant = new IntConstant(coeff*(((IntConstant)r).getValue() - ((IntConstant)((Operation)l).getOperand(0)).getValue()));
									
									oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
								} else {
									//constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									oper = operation;
								}
							} else if (((Operation)l).getOperand(1) instanceof IntConstant && ((Operation)l).getOperand(0) instanceof IntVariable) {
								System.out.println("Case 2");
							} else {
								System.out.println("Case 3: ");
								//oper = operation;
								oper = (Operation) Operation.TRUE;
							}
						} else if (r instanceof Operation) {
							if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntConstant) {
								System.out.println(r + " RIGHT123");
								switch (((Operation) r).getOperator()) {
								case ADD:
									System.out.println("add");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() + ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case SUB:
									System.out.println("sub");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case MUL:
									System.out.println("mul");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() * ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case DIV:
									System.out.println("div");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() / ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								default:
									oper = operation;
									break;
								}
							} else if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntVariable) {
								System.out.println(operation.getOperator() + " " + r);
								System.out.println("Case 1");
								int coeff = 1;
								if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								} 
								if (l instanceof IntConstant) {
									System.out.println("$5");
									constant = new IntConstant(coeff*(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(0)).getValue()));
									
									oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
								} else {
									//constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									oper = operation;
								}
								
								//oper = new Operation(operation.getOperator(), )
							} else if (((Operation)r).getOperand(1) instanceof IntConstant && ((Operation)r).getOperand(0) instanceof IntVariable) {
								System.out.println("Case 2");
							} else {
								System.out.println("Case 3: ");
								//oper = operation;
								oper = (Operation) Operation.TRUE;
							}
						} else if (r instanceof IntConstant) {
							System.out.println("12345");
						} else if (l instanceof IntConstant) {
							System.out.println("123456");
						}
						break;
					case LE:
						System.out.println("Less than/ equals");
						System.out.println(l + " " + r + " tag");
						if (l instanceof IntConstant && r instanceof IntConstant) {
							if (((IntConstant)l).getValue() <= ((IntConstant)r).getValue()) {
								System.out.println("ITS TRUE");
								oper = (Operation) Operation.TRUE;
							} else {
								System.out.println("FALSE");
								oper = (Operation) Operation.FALSE;
							}
						} else if (l instanceof Operation) {
							if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntConstant) {
								switch (((Operation) l).getOperator()) {
								case ADD:
									System.out.println("add");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() + ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									if (specialMap.containsKey(r)) {
										oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
									} else {
										oper = new Operation(operation.getOperator(), constant, r);
									}
									
									break;
								case SUB:
									System.out.println("sub");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() - ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								case MUL:
									System.out.println("mul");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() * ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								case DIV:
									System.out.println("div");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() / ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								default:
									oper = operation;
									break;
								}
							} else if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntVariable) {
								System.out.println(operation.getOperator() + " " + r);
								System.out.println("Case 1");
								int coeff = 1;
								if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								} 
								if (l instanceof IntConstant) {
									System.out.println("$5");
									constant = new IntConstant(coeff*(((IntConstant)r).getValue() - ((IntConstant)((Operation)l).getOperand(0)).getValue()));
									
									oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
								} else {
									//constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									oper = operation;
								}
							} else if (((Operation)l).getOperand(1) instanceof IntConstant && ((Operation)l).getOperand(0) instanceof IntVariable) {
								System.out.println("Case 2");
							} else {
								System.out.println("Case 3: ");
								//oper = operation;
								oper = (Operation) Operation.TRUE;
							}
						} else if (r instanceof Operation) {
							if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntConstant) {
								System.out.println(r + " RIGHT123");
								switch (((Operation) r).getOperator()) {
								case ADD:
									System.out.println("add");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() + ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case SUB:
									System.out.println("sub");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case MUL:
									System.out.println("mul");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() * ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case DIV:
									System.out.println("div");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() / ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								default:
									oper = operation;
									break;
								}
							} else if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntVariable) {
								System.out.println(operation.getOperator() + " " + r);
								System.out.println("Case 1");
								int coeff = 1;
								if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								} 
								if (l instanceof IntConstant) {
									System.out.println("$5");
									constant = new IntConstant(coeff*(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(0)).getValue()));
									
									oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
								} else {
									//constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									oper = operation;
								}
								
								//oper = new Operation(operation.getOperator(), )
							} else if (((Operation)r).getOperand(1) instanceof IntConstant && ((Operation)r).getOperand(0) instanceof IntVariable) {
								System.out.println("Case 2");
							} else {
								System.out.println("Case 3: ");
								//oper = operation;
								oper = (Operation) Operation.TRUE;
							}
						} else if (r instanceof IntConstant) {
							System.out.println("12345");
						} else if (l instanceof IntConstant) {
							System.out.println("123456");
						}
						break;
					case LT:
						System.out.println("Less than");
						System.out.println(l + " " + r + " tag");
						if (l instanceof IntConstant && r instanceof IntConstant) {
							if (((IntConstant)l).getValue() < ((IntConstant)r).getValue()) {
								System.out.println("ITS TRUE");
								oper = (Operation) Operation.TRUE;
							} else {
								System.out.println("FALSE");
								oper = (Operation) Operation.FALSE;
							}
						} else if (l instanceof Operation) {
							if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntConstant) {
								switch (((Operation) l).getOperator()) {
								case ADD:
									System.out.println("add");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() + ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									if (specialMap.containsKey(r)) {
										oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
									} else {
										oper = new Operation(operation.getOperator(), constant, r);
									}
									
									break;
								case SUB:
									System.out.println("sub");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() - ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								case MUL:
									System.out.println("mul");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() * ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								case DIV:
									System.out.println("div");
									constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() / ((IntConstant)((Operation)l).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), constant, r);
									break;
								default:
									oper = operation;
									break;
								}
							} else if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntVariable) {
								System.out.println(operation.getOperator() + " " + r);
								System.out.println("Case 1");
								int coeff = 1;
								if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								} 
								if (l instanceof IntConstant) {
									System.out.println("$5");
									constant = new IntConstant(coeff*(((IntConstant)r).getValue() - ((IntConstant)((Operation)l).getOperand(0)).getValue()));
									
									oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
								} else {
									//constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									oper = operation;
								}
							} else if (((Operation)l).getOperand(1) instanceof IntConstant && ((Operation)l).getOperand(0) instanceof IntVariable) {
								System.out.println("Case 2");
							} else {
								System.out.println("Case 3: ");
								//oper = operation;
								oper = (Operation) Operation.TRUE;
							}
						} else if (r instanceof Operation) {
							if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntConstant) {
								System.out.println(r + " RIGHT123");
								switch (((Operation) r).getOperator()) {
								case ADD:
									System.out.println("add");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() + ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case SUB:
									System.out.println("sub");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case MUL:
									System.out.println("mul");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() * ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								case DIV:
									System.out.println("div");
									constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() / ((IntConstant)((Operation)r).getOperand(1)).getValue());
									System.out.println("TP: " + constant);
									oper = new Operation(operation.getOperator(), l, constant);
									break;
								default:
									oper = operation;
									break;
								}
							} else if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntVariable) {
								System.out.println(operation.getOperator() + " " + r);
								System.out.println("Case 1");
								int coeff = 1;
								if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								} 
								if (l instanceof IntConstant) {
									System.out.println("$5");
									constant = new IntConstant(coeff*(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(0)).getValue()));
									
									oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
								} else {
									//constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
									oper = operation;
								}
								
								//oper = new Operation(operation.getOperator(), )
							} else if (((Operation)r).getOperand(1) instanceof IntConstant && ((Operation)r).getOperand(0) instanceof IntVariable) {
								System.out.println("Case 2");
							} else {
								System.out.println("Case 3: ");
								//oper = operation;
								oper = (Operation) Operation.TRUE;
							}
						} else if (r instanceof IntConstant) {
							System.out.println("12345");
						} else if (l instanceof IntConstant) {
							System.out.println("123456");
						}
						break;
					case NE:
						System.out.println("Not equals");
						if (l instanceof IntConstant && r instanceof IntConstant) {
							if (((IntConstant)l).getValue() != ((IntConstant)r).getValue()) {
								System.out.println("ITS TRUE");
								oper = (Operation) Operation.TRUE;
							} else if (((IntConstant)l).getValue() == ((IntConstant)r).getValue()) {
								System.out.println("FALSE");
								oper = (Operation) Operation.FALSE;
							} else {
								oper = operation;
							}
						}
						break;
					default:
						break;
					}
					stack.push(oper);
					System.out.println(stack + " : STACK");
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				Expression shrink = null;
				System.out.println(l + " " + op + " " + r);
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
					System.out.println(l + " ** " + r);
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), specialMap.get(r)));
						System.out.println("here 1");
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(op, l, specialMap.get(r)));
						System.out.println("here 2");
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l) , r));
						System.out.println("here 3");
					} else {
						stack.push(new Operation(op, l, r));
					} 
				}
			} else {
				System.out.println("poppy boi");
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}
	}

}
