package za.ac.sun.cs.green.service.simplify;



import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;



public class ConstantPropagation extends Visitor {
	
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
		
		System.out.println(o4);
		
		OrderingVisitor ov = new OrderingVisitor();
		try {
			o4.accept(ov);
			
			//ov.postVisit(o4);
			ov.print();
			System.out.println(ov.getExpression());
			
		} catch (VisitorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		test01();
	}
	
	public static void test01() {
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
		OrderingVisitor ov = new OrderingVisitor();
		
		try {
			o.accept(ov);
			ov.print();
		} catch (VisitorException e) {
			e.printStackTrace();
		}
	}

	static class OrderingVisitor extends Visitor {
		Map<Expression, Expression> exp = new HashMap<>();
		
		private Stack<Expression> stack;

		public OrderingVisitor() {
			System.out.println("Stack: " + stack);
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			System.out.println("EXPRESSION ON DAT BOI");
			return stack.pop();
		}
		
		public void pushh(Operation.Operator op, IntVariable l, IntConstant r) {
			if (exp.containsKey(r)) {
				stack.push(new Operation(op, l, exp.get(r)));
			} else {
				stack.push(new Operation(op, l, r));
			}
		}
		
		public void pushh(Operation.Operator op, IntConstant l, IntVariable r) {
			if (exp.containsKey(l)) {
				stack.push(new Operation(op, l, exp.get(r)));
			} else {
				stack.push(new Operation(op, l, r));
			}
		}

		@Override
		public void postVisit(IntConstant constant) throws VisitorException {
			System.out.println("Constant: " + constant);
			
			stack.push(constant);
			System.out.println("Stack: " + stack);
		}

		@Override
		public void postVisit(IntVariable variable) {
			System.out.println("Variable: " + variable);
			stack.push(variable);
			System.out.println("Stack: " + stack);
		}
		
		public void print() {
			System.out.println(exp.size());
			System.out.println("HEEERE");
			System.out.println(exp);
			System.out.println(stack);
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			System.out.println(stack.size());
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

				//1 + y = 10, equates to y==9
				if (nop.equals(Operation.Operator.EQ) && r instanceof IntConstant && l instanceof Operation) {
					IntConstant nr = new IntConstant(-1*Integer.parseInt(r.toString()));
					System.out.println(nr + " new r");
					Operation test = (Operation) l;

					r = Operation.apply(Operation.Operator.ADD, test.getOperand(0), nr);
					r = new IntConstant(-1*(Integer.parseInt(r.toString())));
				
					if (test.getOperand(0) instanceof IntConstant) {
						l = test.getOperand(1);
					} else {
						l = test.getOperand(0);
					}
					new Operation(Operation.Operator.EQ, l, r);
					//12 + y = -1, equates to y = -13
				} else if (nop.equals(Operation.Operator.EQ) && l instanceof IntConstant && r instanceof Operation) {
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
					new Operation(Operation.Operator.EQ, r, l);
				}
				
				if (nop.equals(Operation.Operator.EQ) && l instanceof IntConstant && r instanceof Operation) {
					System.out.println("YEET 2");
				}
				if (nop.equals(Operation.Operator.EQ) && r instanceof IntVariable && l instanceof IntConstant) {
					System.out.println("WHAT 1");
					exp.put(r, l);
				} else if (nop.equals(Operation.Operator.EQ) && l instanceof IntVariable && r instanceof IntConstant) {
					System.out.println("WHAT 2");
					exp.put(l, r);
				} else {
					System.out.println("WHAT 3");
				}
				
				
				if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					if (exp.containsKey(r)) {
						System.out.println("Contains a value");
					} else if (exp.containsKey(l)) {
						System.out.println("Contains this shit");
					}
					stack.push(new Operation(nop, r, l));
					System.out.println("Stack: " + stack);
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					stack.push(new Operation(nop, r, l));
					exp.put(l, r);
				} else if ((l instanceof IntVariable)
						&& (r instanceof IntConstant)) {
					//System.out.println("Q " + nop + " | " + l + " | " + r);
					stack.push(new Operation(nop, l, r));
					System.out.println("Stack: " + stack);
					//exp.put(l, r);
				} 
				else {
					stack.push(new Operation(nop, l ,r));
					System.out.println("Stack: " + stack);
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				if (exp.containsKey(r)) {
					stack.push(new Operation(op, l, exp.get(r)));
				} else if (exp.containsKey(l)) {
					stack.push(new Operation(op, exp.get(l), r));
				} else {
					stack.push(new Operation(op, l, r));
				} 
				exp.put(l, r);
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}

	}
}
