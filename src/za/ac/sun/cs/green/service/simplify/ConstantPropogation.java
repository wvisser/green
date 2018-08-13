package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropogation extends BasicService {

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Expression e = simplify(instance.getFullExpression());
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	public Expression simplify(Expression expression) {
		try {
			SimplificationVisitor simplificationVisitor = new SimplificationVisitor();
			expression.accept(simplificationVisitor);
			Expression simplified = simplificationVisitor.getExpression();
			return simplified;
		} catch (VisitorException x) {
			System.out.println("Houston, we have a problem!" + x);
		}
		return null;
	}

	private static class SimplificationVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<Expression, Constant> knownValues;

		public SimplificationVisitor() {
			stack = new Stack<Expression>();
			knownValues = new HashMap<Expression, Constant>();
		}

		public Expression getExpression() {
			Expression top = stack.pop();
			top = propogateConstants(top);
			return top;
		}

		@Override
		public void postVisit(Variable variable) {
			pushAndPrint(variable);
		}

		@Override
		public void postVisit(Constant constant) {
			pushAndPrint(constant);
		}

		@Override
		public void postVisit(Operation operation) {
			if (operation.getOperator().equals(Operation.Operator.EQ)) {
				Expression l = operation.getOperand(0);
				Expression r = operation.getOperand(1);
				if (r instanceof Constant) {
					final Constant constVal = (Constant) r;
					if (l instanceof Variable) {
						final Variable constVar = (Variable) l;
						knownValues.put(constVar, constVal);
					}
				}
			}
			pushAndPrint(operation);
		}

		private Expression propogateConstants(Expression expression) {

			if (expression instanceof Constant) {
				return expression;
			}

			if (expression instanceof Variable) {
				if (knownValues.get(expression) != null) {
					return knownValues.get(expression);
				} else {
					return expression;
				}
			}

			if (expression instanceof Operation) {
				Operation op = (Operation) expression;
				Expression l = op.getOperand(0);
				Expression r = op.getOperand(1);

				// Handle variable assignment as a special case so we don't end up with val == val
				if (op.getOperator().equals(Operation.Operator.EQ)) {
					if (l instanceof Variable && r instanceof Constant) {
						return op;
					}
				}

				l = propogateConstants(l);
				r = propogateConstants(r);
				Operation newop = new Operation(op.getOperator(), l, r);
				return newop;
			}

			System.out.println("We should never get to this point!");
			return expression;
		}

		// For debugging purposes
		private void pushAndPrint(Expression e) {
			// System.out.println("Pushing expression to stack: " + e);
			stack.push(e);
		}
	}
}
