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

	public Expression simplify(Expression expression,Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Simplify: " + expression);
			invocations++;
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			log.log(Level.FINEST, "After Simplify: " + expression);
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return expression;
	}

	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<IntVariable, IntConstant> map;

		public OrderingVisitor() {
			stack = new Stack<Expression>();
			map = new TreeMap<IntVariable, IntConstant>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();
			//Expression right = stack.pop();
			//Expression left = stack.pop();
			//System.out.println("Left:" + left + " Right: " + right);

			Operation.Operator nop = null;

				switch (op) {
				case EQ:
					System.out.println("need to check for equality");
					break;
				case NE:
					System.out.println("need to check for nonequality");
					nop = Operation.Operator.NE;
					break;
				case LT:
					System.out.println("need to check for less than");
					nop = Operation.Operator.GT;
					break;
				case LE:
					System.out.println("need to check for less than equals");
					nop = Operation.Operator.GE;
					break;
				case GT:
					System.out.println("need to check for greater than");
					nop = Operation.Operator.LT;
					break;
				case GE:
					System.out.println("need to check for greater than equals");
					nop = Operation.Operator.LE;
					break;
				case ADD:
					System.out.println("need to check for addition");
					//stack.push();
					break;
				case AND:
					System.out.println("need to check for and");
					nop = Operation.Operator.AND;
					break;
				default:
					System.out.println("Default case!");
					break;
				}
    //
		// 	if (nop != null) {
		// 		Expression r = stack.pop();
		// 		Expression l = stack.pop();
		// 		if ((r instanceof IntVariable)
		// 				&& (l instanceof IntVariable)
		// 				&& (((IntVariable) r).getName().compareTo(
		// 						((IntVariable) l).getName()) < 0)) {
		// 			stack.push(new Operation(nop, r, l));
		// 		} else if ((r instanceof IntVariable)
		// 				&& (l instanceof IntConstant)) {
		// 			stack.push(new Operation(nop, r, l));
		// 		} else {
		// 			stack.push(operation);
		// 		}
		// 	} else if (op.getArity() == 2) {
		// 		Expression r = stack.pop();
		// 		Expression l = stack.pop();
		// 		stack.push(new Operation(op, l, r));
		// 	} else {
		// 		for (int i = op.getArity(); i > 0; i--) {
		// 			stack.pop();
		// 		}
		// 		stack.push(operation);
		// 	}
		// }
		}
	}
}
