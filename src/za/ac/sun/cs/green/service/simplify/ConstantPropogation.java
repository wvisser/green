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
			log.log(Level.FINEST, "Before ConstantPropagation: " + expression);
			invocations++;
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			//CanonizationVisitor canonizationVisitor = new CanonizationVisitor();
			//expression.accept(canonizationVisitor);
			//Expression canonized = canonizationVisitor.getExpression();

			log.log(Level.FINEST, "After ConstantPropagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
			"encountered an exception -- this should not be happening!",
			x);
		}
		return null;
	}
	//if variable already on stack, dont push variable, push it's value
	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;
		private HashMap<Expression,Expression> hashMap;

		public OrderingVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			System.out.println("***Expression**");
			Expression popExpr = stack.pop();
			System.out.println("Expression popped: " + popExpr);
			return popExpr;
		}

		@Override
		public void postVisit(IntConstant constant) {
			System.out.println("***Constant**");
			System.out.println("Post visit constant: " + constant);
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			System.out.println("***INTVariable**");
			System.out.println("Post visit variable: " + variable);
			// if (hashMap.containsKey(variable)) {
			// 	Expression value = hashMap.get(variable);
			// 	stack.push(value);
			// } else {
			stack.push(variable);
		}
	}

	@Override
	public void postVisit(Operation operation) throws VisitorException {
		System.out.println("***Operation**");
		System.out.println("Post vist operation: " + operation);
		Operation.Operator op = operation.getOperator();
		Operation.Operator nop = null;
		switch (op) {
			case EQ:
			// if (op.PREFIX) {
			// 	hashMap.put(op.PREFIX, op.POSTFIX);
			// }
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
			System.out.println("Pushed operation: "+operation);
			stack.push(operation);
		}
	}

}
