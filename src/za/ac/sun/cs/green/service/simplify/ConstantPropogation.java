package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

public class ConstantPropogation extends BasicService {

	/**
	 * Number of times the slicer has been invoked.
	 */
	private int invocations = 0;

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = canonize(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	public Expression canonize(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			//OrderingVisitor orderingVisitor = new OrderingVisitor();
			//expression.accept(orderingVisitor);
			//expression = orderingVisitor.getExpression();
			//CanonizationVisitor canonizationVisitor = new CanonizationVisitor();
			//expression.accept(canonizationVisitor);
			//Expression canonized = canonizationVisitor.getExpression();
			Expression canonized = expression;
			if (canonized != null) {
				canonized = new Renamer(map).rename(canonized);
			}
			log.log(Level.FINEST, "After Canonization: " + canonized);
			return canonized;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;

		public OrderingVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
			System.out.println("\n... 1: " + constant + " ...");
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
			System.out.println("\n... 2: " + variable + " ...");
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
				if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, r, l));
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					stack.push(new Operation(nop, r, l));
				} else {
					stack.push(operation);
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
			System.out.println("\n... 3: " + operation + " ...");
		}
		
	}
	
	private static class Renamer extends Visitor {

		private Map<Variable, Variable> map;

		private Stack<Expression> stack;

		public Renamer(Map<Variable, Variable> map) {
			this.map = map;
			stack = new Stack<Expression>();
		}

		public Expression rename(Expression expression) throws VisitorException {
			System.out.println("heyhey: " + this);
			expression.accept(this);
			return stack.pop();
		}

		@Override
		public void postVisit(IntVariable variable) {
			//Variable v = map.get(variable);
			//System.out.println("4: get this from map: " + v + "\n");
//			if (v == null) {
//				v = new IntVariable("v" + map.size(), variable.getLowerBound(),
//						variable.getUpperBound());
//				map.put(variable, v);
//				System.out.println("4: put this in map: " + variable + " with " + v + "\n");
//			}
			stack.push(variable);
			System.out.println("4: push this to stack: " + variable + "\n");
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
			System.out.println("5: push this to stack: " + constant + "\n");
		}

		@Override
		public void postVisit(Operation operation) {
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
				System.out.println("6: pop this from stack: " + operands[i-1] + "\n");
			}
			
			if (operands[0].getClass().equals(IntVariable.class) && operands[1].getClass().equals(IntConstant.class)) {
				Variable v = map.get(operands[0]);
				if (v == null) {
					System.out.println("HERE1");
					Variable value = new IntVariable(operands[1].toString(), 0, 99999);
					Variable var = new IntVariable(operands[0].toString(), 0, 99999);
					map.put(var, value);
					//operands[0] = operands[1];
				}
			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntConstant.class)) {
				Variable v = map.get(operands[1]);
				if (v == null) {
					System.out.println("HERE2");
					Variable value = new IntVariable(operands[0].toString(), 0, 99999);
					Variable var = new IntVariable(operands[1].toString(), 0, 99999);
					map.put(var, value);
					//operands[1] = operands[0];
				}
			}
			
			
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntVariable.class)) {
				Variable v = map.get(operands[0]);
				if (v != null) {
					operands[0] = v;
				}
				v = map.get(operands[1]);
				if (v != null) {
					operands[1] = v;
				}
			}
//			
//			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntVariable.class)) {
//				Variable v = map.get(operands[0]);
//				if (v == null) {
//					System.out.println("HERE4");
//					Variable value = new IntVariable(operands[1].toString(), 0, 99999);
//					Variable var = new IntVariable(operands[0].toString(), 0, 99999);
//					map.put(var, value);
//				}
//			}
//			
//			if (operands[1].getClass().equals(IntConstant.class) && operands[0].getClass().equals(IntConstant.class)) {
//				Variable v = map.get(operands[0]);
//				if (v == null) {
//					System.out.println("HERE5");
//					Variable value = new IntVariable(operands[1].toString(), 0, 99999);
//					Variable var = new IntVariable(operands[0].toString(), 0, 99999);
//					map.put(var, value);
//				}
//			}
//			
//			if (operands[1].getClass().equals(IntConstant.class) && operands[0].getClass().equals(IntConstant.class)) {
//				Variable v = map.get(operands[1]);
//				if (v == null) {
//					System.out.println("HERE6");
//					Variable value = new IntVariable(operands[0].toString(), 0, 99999);
//					Variable var = new IntVariable(operands[1].toString(), 0, 99999);
//					map.put(var, value);
//				}
//			}
			
			for (Map.Entry<Variable, Variable> entry : map.entrySet()) {
			    System.out.println("-- " + entry.getKey() + ":" + entry.getValue().toString());
			}
			
			stack.push(new Operation(operation.getOperator(), operands));
			System.out.println("6: push this operation part1 to stack: " + operation + "\n");
			System.out.println("6: push this operation part1 to stack: " + operands[0] + "\n");
			System.out.println("6: push this operation part1 to stack: " + operands[1] + "\n");
		}

	}
	
}
