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
import java.util.Iterator;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation extends BasicService {

	/**
	 * Number of times the slicer has been invoked.
	 */
	private int invocations = 0;

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = simplify(instance.getFullExpression(), map);
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

	public Expression simplify(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Simplification: " + expression);
			invocations++;
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			System.out.println("Expression after orderingVisitor: " + expression);
			// SimplificationVisitor SimplificationVisitor = new SimplificationVisitor();
			// expression.accept(SimplificationVisitor);
			// Expression simplified = SimplificationVisitor.getExpression();
      // System.out.println("-------SIMPLIFIED--------");
      // System.out.println("simplified: " + simplified);
			// if (simplified != null) {
			// 	simplified = new Renamer(map,
			// 	SimplificationVisitor.getVariableSet()).rename(simplified);
			// }
			// log.log(Level.FINEST, "After Simplification: " + simplified);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<Expression, Expression> variableMap = new HashMap<Expression, Expression>();

		public OrderingVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			System.out.println("postVisit constant: " + constant);
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			System.out.println("postVisit variable: " + variable);
			Expression key = variable;
			if (variableMap.containsKey(key)){
				System.out.println("pushing: " + variableMap.get(key));
				stack.push(variableMap.get(key));
			} else {
					stack.push(variable);
			}
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			System.out.println("postVisit operation: " + operation);
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
				System.out.println("Expression r: " + r);
				Expression l = stack.pop();
				System.out.println("Expression l: " + l);
				if (nop == Operation.Operator.EQ) {
					System.out.println("Adding key: " + l + " with value: " + r);
					variableMap.put(l, r);
				}
				if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, r, l));
					System.out.println("Pushing " + l + nop + r);
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					stack.push(new Operation(nop, r, l));
					System.out.println("Pushing " + l + nop + r);
				} else {
					stack.push(new Operation(nop, l , r));
					System.out.println("Pushing operation: " + l + nop + r);
				}
			} else if (op.getArity() == 2) {
				System.out.println("---------IN GETARITY---------");
				Expression r = stack.pop();
				System.out.println("Expression r: " + r);
				Expression l = stack.pop();
				System.out.println("Expression l: " + l);
				stack.push(new Operation(op, l, r));
				System.out.println("Pushing " + l + op + r);
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
				System.out.println("Pushing operation (else)" + operation);
			}
		}

	}
}
