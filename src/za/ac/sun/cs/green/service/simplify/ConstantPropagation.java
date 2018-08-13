package za.ac.sun.cs.green.service.simplify;



import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Operation.Operator;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;


public class ConstantPropagation extends BasicService {
	private int invocations = 0;
	static Map<Expression, Expression> map = new HashMap<>();
	
	public static void main (String[] args) {
		
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
		
		testo();
//		System.out.println(o4);
//		constantVisitor ov = new constantVisitor();
//		try {
//			o4.accept(ov);
//			
//			//ov.postVisit(o4);
//			ov.print();
//			//System.out.println(ov.getExpression());
//			
//		} catch (VisitorException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		//Operation constantProp = (Operation)ov.getExpression();
//		simplificationVisitor sv = new simplificationVisitor();
//		try {
//			ov.getExpression().accept(sv);
//		} catch (VisitorException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		sv.print();
//		System.out.println(map);
//		test01();
	}
	
	public static void test02() {
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
		System.out.println(o);
		constantVisitor ov = new constantVisitor();
		
		try {
			o.accept(ov);
			ov.print();
		} catch (VisitorException e) {
			e.printStackTrace();
		}
	}
	
	public static void basic_test() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntConstant c1 = new IntConstant(2);
		IntConstant c2 = new IntConstant(1);
		IntConstant neg_c = new IntConstant(-2);
		/* All tests passed
		 * x = 2 + 1
		 * x = -2 - 1
		 * x + 1 = 2
		 * x - 1 = 2
		 * 1 - x = 2
		 * 1 + x = 2
		 * -x = 2 + 1
		 * -x = 2 - 1
		 * x + 1 = - 2
		 * x - 1 = - 2
		 * 1 - x = - 2
		 * 1 + x = - 2
		 */
		Operation o1 = new Operation(Operation.Operator.SUB, Operation.ZERO, x);
		//Operation o2 = new Operation(Operation.Operator.SUB, o2, x);
		Operation o = new Operation(Operation.Operator.EQ, o1, neg_c);
		System.out.println(o);
		constantVisitor ov = new constantVisitor();
		try {
			o.accept(ov);
			ov.print();
		} catch (VisitorException e) {
			e.printStackTrace();
		}
	}
	
	public static void testo() {
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
		constantVisitor ov = new constantVisitor();
		try {
			o.accept(ov);
			ov.print();
		} catch (VisitorException e) {
			e.printStackTrace();
		}
	}
	
	public static void test01() {
		IntVariable x = new IntVariable("x", 0, 99);
		IntVariable y = new IntVariable("y", 0, 99);
		IntConstant c = new IntConstant(1);
		IntConstant c2 = new IntConstant(10);
		IntConstant c3 = new IntConstant(2);
		IntConstant c4 = new IntConstant(4);
		Operation o1 = new Operation(Operation.Operator.EQ, x, c); // o1 : (x = 1)
		Operation o2 = new Operation(Operation.Operator.ADD, x, y); // o2 : x + y
		Operation o3 = new Operation(Operation.Operator.LT, o2, c2); // o3 : (x+y) < 10
		Operation oi = new Operation(Operation.Operator.SUB, y, c); // oi : y-1
		Operation o4 = new Operation(Operation.Operator.EQ, oi, c3); // o4 : y-1 = 2
		Operation o5 = new Operation(Operation.Operator.AND, o1, o3); // o5 : (x = 1) && (x+y < 10)
		Operation o = new Operation(Operation.Operator.AND, o5, o4); // o = (x = 1) && (x+y < 10) && (y-1 = 2)
		
		Operation t = new Operation(Operation.Operator.SUB, c3, x);
		Operation test = new Operation(Operation.Operator.EQ, t, c2);
		System.out.println(test);
		// (x = 1) && (x+y < 10) && (y-1 = 2)
		//check(o, "(x==1)&&(y==3)");
		constantVisitor ov = new constantVisitor();
		
		try {
			test.accept(ov);
			ov.print();
		} catch (VisitorException e) {
			e.printStackTrace();
		}
//		Operation constantProp = (Operation)ov.getExpression();
//		simplificationVisitor sv = new simplificationVisitor();
//		try {
//			constantProp.accept(sv);
//		} catch (VisitorException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		sv.print();
	}

	public ConstantPropagation(Green solver) {
		super(solver);
	}
	
	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagate(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}
	
	public Expression propagate(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			constantVisitor constantVisitor = new constantVisitor();
			expression.accept(constantVisitor);
			expression = constantVisitor.getExpression();
			simplificationVisitor canonizationVisitor = new simplificationVisitor();
			expression.accept(canonizationVisitor);
			Expression canonized = canonizationVisitor.getExpression();
			
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
		private static Map<Expression, Expression> specialMap = new HashMap<>();
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
		public void preVisit(IntConstant constant) throws VisitorException {
			System.out.println(constant + " constant preVisit");
		}
		
		@Override
		public void preVisit(IntVariable variable) throws VisitorException {
			System.out.println(variable + " variable preVisit");
		}
		
		@Override
		public void preVisit(Operation operation) throws VisitorException {
			if (operation.getOperator().equals(Operation.Operator.EQ)) {
				if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntConstant) {
					System.out.println("PRE VISIT 1 " + operation);
					specialMap.put(operation.getOperand(0), operation.getOperand(1));
				} else if (operation.getOperand(0) instanceof IntConstant && operation.getOperand(1) instanceof IntVariable) {
					System.out.println("PRE VISIT 2 " + operation);
					specialMap.put(operation.getOperand(1), operation.getOperand(0));
				} else if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntVariable) {
					specialMap.put(operation.getOperand(0), operation.getOperand(1));
				}
				
			}
			if (operation.getOperand(0) instanceof IntVariable || operation.getOperand(1) instanceof IntVariable) {
				System.out.println("OPER : " + operation);
				System.out.println(specialMap + " SM");
				Object t[] = specialMap.keySet().toArray();
			
				for (int i = t.length-1; i >= 0; i--) {
					//System.out.println(specialMap.get(specialMap.keySet()));
					if (specialMap.containsKey(specialMap.get(t[i]))) {
						System.out.println(t[i] + " ---> " + specialMap.get(specialMap.get(t[i])));
						specialMap.replace((IntVariable) t[i], specialMap.get(specialMap.get(t[i])));
					}
					
					//specialMap.replace(t[i], );
					
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
				
				if (nop.equals(Operation.Operator.EQ) && r instanceof IntVariable && l instanceof IntConstant) {
					//map.put(r, l);
				} else if (nop.equals(Operation.Operator.EQ) && l instanceof IntVariable && r instanceof IntConstant) {
					//map.put(l, r);
				} 
				IntConstant constant = null;
				IntVariable variable = null;
				Operation oper = null;
				//Equate function
				if (nop.equals(Operation.Operator.EQ)) {
					if (l instanceof Operation) {
						if (r instanceof IntVariable) {
							System.out.println("1");
							if (((Operation) l).getOperand(0) instanceof IntConstant && ((Operation) l).getOperand(1) instanceof IntConstant) {
								//Check operator
								if (((Operation) l).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() + ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() - ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								}
							} 
							oper = new Operation(Operation.Operator.EQ, constant, r);
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
								}
							} 
							oper = new Operation(Operation.Operator.EQ, l, constant);
						} else if (l instanceof IntConstant) {
							System.out.println("4");
							if (((Operation) r).getOperand(0) instanceof IntConstant) {
								int coeff = 1;
								if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									System.out.println("Negative coeff");
									coeff = -1;
								}
								variable = (IntVariable) ((Operation) r).getOperand(1);
								System.out.println("Left is constant " + ((Operation) r).getOperator());
								constant = new IntConstant(coeff*(((IntConstant) l).getValue() - ((IntConstant) ((Operation) r).getOperand(0)).getValue()));
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
							}
						
							oper = new Operation(Operation.Operator.EQ, constant, variable);
							
						}
						stack.push(oper);
					} else if (l instanceof IntConstant && r instanceof IntConstant) {
						//Check function
						if (op.equals(Operation.Operator.ADD)) {
							constant = new IntConstant(((IntConstant)(l)).getValue() + ((IntConstant)(r)).getValue());
						} else if (op.equals(Operation.Operator.SUB)) {
							constant = new IntConstant(((IntConstant)(l)).getValue() - ((IntConstant)(r)).getValue());
						}
					} else {
						stack.push(new Operation(nop, l, r));
					}
					System.out.println("Constant: " + constant + "\nVariable: " + variable + "\nOperation: " + oper);
					
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, l, r));
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					stack.push(new Operation(nop, l, r));
				} else if ((l instanceof IntVariable)
						&& (r instanceof IntConstant)) {
					stack.push(new Operation(nop, l, r));
				} 
				else {
					stack.push(new Operation(op, l ,r));
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				if (specialMap.containsKey(r)) {
					stack.push(new Operation(op, l, specialMap.get(r)));
				} else if (specialMap.containsKey(l)) {
					stack.push(new Operation(op, specialMap.get(l) , r));
				} else {
					stack.push(new Operation(op, l, r));
				} 
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}
	}
	
	private static Operation.Operator reverse(Operation.Operator in) {
		if (in.equals(Operation.Operator.ADD)) {
			return Operation.Operator.SUB;
		} else {
			return Operation.Operator.ADD;
		}	
	}
	
	
	static class simplificationVisitor extends Visitor {
		private Stack<Expression> stack;

		public simplificationVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}
		
		public void print() {
			System.out.println("YAS SIMPLIFY");
			System.out.println(stack);
			System.out.println(stack.peek());
		}

		@Override
		public void postVisit(IntConstant constant) throws VisitorException {
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
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

				if (nop.equals(Operation.Operator.EQ) && r instanceof IntConstant && l instanceof Operation) {
					Operation left = (Operation) l;
					Stack<Object> stack_temp = new Stack<Object>();
					
						
					stack_temp.push(r); //push constant
					
					stack_temp.push(reverse(left.getOperator()));
					if (left.getOperand(0) instanceof IntConstant) {
						stack_temp.push(left.getOperand(0));
					} else if (left.getOperand(1) instanceof IntConstant) {
						stack_temp.push(left.getOperand(1));
					}
					stack_temp.push(Operation.Operator.EQ);
					if (left.getOperand(0) instanceof IntVariable) {
						stack_temp.push(left.getOperand(0));
					} else if (left.getOperand(1) instanceof IntVariable) {
						stack_temp.push(left.getOperand(1));
					}
				
					System.out.println(stack_temp);
					Expression constant = null;
					IntConstant ln = null;
					IntVariable orig = null;
					
					while (!stack_temp.isEmpty()) {
						if (stack_temp.peek() != Operation.Operator.EQ && stack_temp.peek() != Operation.Operator.ADD && stack_temp.peek() != Operation.Operator.SUB) {
							if (stack_temp.peek() instanceof IntConstant) {
								ln = (IntConstant) stack_temp.pop();
								
								System.out.print(ln);
								System.out.println(stack_temp);
							} else {
								
									orig = (IntVariable) stack_temp.pop();
									System.out.println(orig + " 123");
									System.out.println(stack_temp);
								
							}	
						} else if (stack_temp.peek() == Operation.Operator.EQ) {
							System.out.print(" " + stack_temp.pop() + " ");
							System.out.println(stack_temp);
						} else {
							Operation.Operator t = (Operator) stack_temp.pop();
							System.out.println(stack_temp);
							IntConstant rn = (IntConstant)(stack_temp.pop());
							if (reverse(t).equals(Operation.Operator.ADD)) {
								ln = new IntConstant(ln.getValue()*-1);
								t = reverse(t);
							} 
							constant = Operation.apply(t, rn, ln);
							System.out.println(constant + " CONSTANT");
							if (orig != null) {
							//bobby.put(orig, constant);
							}
						}
					}
					System.out.println("ORIG:" + orig + " CONSTANT: " + constant);
					//exp.put(orig, constant);
			
					System.out.println("WHAT");
					Operation fin = new Operation(Operation.Operator.EQ, orig, constant);
				
					System.out.println(fin + " WHAT!");
					stack.push(fin);
					//exp.put(fin.getOperand(0), fin.getOperand(1));
					System.out.println(" R R R " + r);
					if (left.getOperand(0) instanceof IntConstant) {
						System.out.println(left.getOperator() + " OPERATOR");
						if (left.getOperand(1) instanceof IntVariable) {
							System.out.println("sn " + left.getOperand(0));
						}
						System.out.println("Preright: " + r + " Preleft: " + -1*Integer.parseInt(left.getOperand(0).toString()));
						IntConstant lv = new IntConstant(-1*Integer.parseInt(left.getOperand(0).toString()));
						r = Operation.apply(Operation.Operator.ADD, r, lv);
						l = new IntConstant(-1*Integer.parseInt(left.getOperand(0).toString()));
						
						System.out.println("Left:" + l + " " + "\tRight: " + r);
					} else {
						System.out.println(left.getOperator() + " OPERATOR +");
						r = Operation.apply(Operation.Operator.ADD, left.getOperand(1), r);
						l = left.getOperand(0);
					}
				} else if (nop.equals(Operation.Operator.EQ) && l instanceof IntConstant && r instanceof Operation) {
					System.out.println("ISSUES");
					IntConstant nl = new IntConstant(-1*Integer.parseInt(l.toString()));
					System.out.println(nl);
					Operation test = (Operation) r;

					l = Operation.apply(Operation.Operator.ADD, test.getOperand(0), nl);
					l = new IntConstant(-(Integer.parseInt(l.toString())));
					if (test.getOperand(0) instanceof IntConstant) {
						r = test.getOperand(1);
					} else {
						r = test.getOperand(0);
					}
					new Operation(Operation.Operator.EQ, l, r);
				} else {
				
			
				if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, l, r));
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					stack.push(new Operation(nop, l, r));
				} else if ((l instanceof IntVariable)
						&& (r instanceof IntConstant)) {
					stack.push(new Operation(nop, l, r));
				} 
				else {
					stack.push(new Operation(op, l , r));
				}
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				stack.push(new Operation(op, l, r));
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
			
		}
	}
}
