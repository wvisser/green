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
		private Map<Variable, Constant> knownVariables;

		public SimplificationVisitor() {
			stack = new Stack<Expression>();
			knownVariables = new HashMap<Variable, Constant>();
		}

		public Expression getExpression() {
			printStack();
			return stack.pop();
		}

		@Override
		public void postVisit(Variable variable) {
			System.out.println("Visited variable " + variable);
			if (knownVariables.get(variable) != null) {
				System.out.println("Variable is known");
			}
			stack.push(variable);
		}

		@Override
		public void postVisit(Constant constant) {
			System.out.println("Visited constant " + constant);
			stack.push(constant);
		}

		@Override
		public void postVisit(Operation operation) {
			System.out.println("Visited operation " + operation);
			Expression top = stack.peek();
			if (operation.getOperator().equals(Operation.Operator.EQ)) {
				// TODO: check for IntConstant instead?
				if (top instanceof Constant) {
					final Constant constVal = (Constant) stack.pop();
					top = stack.peek();
					if (top instanceof Variable) {
						final Variable constVar = (Variable) stack.pop();
						System.out.println(constVar + " is equal to " + constVal);
						knownVariables.put(constVar, constVal);
						return;
					} else {
						stack.push(top);
					}
				}
			}
			stack.push(operation);
		}

		// For debugging purposes
		private void printStack() {
			Stack<Expression> otherStack = new Stack<Expression>();
			System.out.println("Top of stack");
			for (int i = 0; i < stack.size(); i++) {
				Expression expr = stack.pop();
				otherStack.push(expr);
				System.out.println(expr);
			}
			for (int i = 0; i < otherStack.size(); i++) {
				stack.push(otherStack.pop());
			}
		}
	}
}
