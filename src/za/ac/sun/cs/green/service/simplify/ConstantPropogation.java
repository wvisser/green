package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.Set;
import java.util.Stack;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.expr.Constant;
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
			System.out.println("Before simplification: " + expression);		// XXX
			SimplificationVisitor simplificationVisitor = new SimplificationVisitor();
			expression.accept(simplificationVisitor);
			Expression simplified = simplificationVisitor.getExpression();
			System.out.println("After simplification: " + expression);		// XXX
			return simplified;
		} catch (VisitorException x) {
			System.out.println("Houston, we have a problem!" + x);
		}
		return null;
	}

	private static class SimplificationVisitor extends Visitor {

		private Stack<Expression> stack;

		public SimplificationVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(Variable variable) {
			System.out.println("Pushing variable " + variable);
			stack.push(variable);
		}

		@Override
		public void postVisit(Constant constant) {
			System.out.println("Pushing constant " + constant);
			stack.push(constant);
		}

		@Override
		public void postVisit(Operation operation) {
			System.out.println("Pushing operation " + operation);
			System.out.println(operation.getOperator().equals(Operation.Operator.EQ));
			stack.push(operation);
		}
	}
}
