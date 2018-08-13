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

public class ConstantPropagation extends BasicService {
	/**
	 * Number of times the slicer has been invoked.
	 */
	private  int invocations = 0;

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = constantPropagation(instance.getFullExpression(), map);
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

	
	public Expression constantPropagation(Expression expression, Map<Variable, Variable> map) {
		try {
			invocations++;
			PropogateVisitor propVisitor = new PropogateVisitor();
			expression.accept(propVisitor);
			Expression simplified = propVisitor.getExpression();	
			log.log(Level.FINEST, "After simplification: " + expression);					
			System.out.println("Simplified: " + simplified);
			return simplified;
		} catch (VisitorException e) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", e);
		}
		return null;
	}

	private static class PropogateVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<IntVariable, IntConstant>  variables;

		public PropogateVisitor() {
			stack = new Stack<Expression>();
			variables = new HashMap<>();
		}

		public Expression getExpression() {
			Expression e = stack.pop();
			System.out.println("Final expression " + e);
			return e;
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			// check if variable already exists.
			if (variables.containsKey(variable)) {
				System.out.println("Variable already assigned constant, " + variable.getName() + " = "+ variables.get(variable));
				stack.push(variables.get(variable));
			} else {
				System.out.println("Variable not assigned constant");
				stack.push(variable);
			}			
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

				if (nop == Operation.Operator.EQ) {
					if ((r instanceof IntVariable) && (l instanceof IntConstant)) {							
						System.out.println("Variables placed in hashmap, " + operation);
						stack.push(new Operation(nop, r, l));
						variables.put((IntVariable) r, (IntConstant) l);												
					} else if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
						System.out.println("Variables placed in hashmap, " + operation);
						stack.push(new Operation(nop, l, r));
						variables.put((IntVariable) l, (IntConstant) r);											
					} else {
						stack.push(new Operation(nop, l, r));
					}
				} else {
					if (variables.containsKey(r)) {
						r = variables.get(r);
					}
					if (variables.containsKey(l)) {
						l = variables.get(l);
					}
					stack.push(new Operation(nop, l, r));
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

	private static class Renamer extends Visitor {

		private Map<Variable, Variable> map;

		private Stack<Expression> stack;

		public Renamer(Map<Variable, Variable> map,
				SortedSet<IntVariable> variableSet) {
			this.map = map;
			stack = new Stack<Expression>();
		}

		public Expression rename(Expression expression) throws VisitorException {
			expression.accept(this);
			return stack.pop();
		}

		@Override
		public void postVisit(IntVariable variable) {
			Variable v = map.get(variable);
			if (v == null) {
				v = new IntVariable("v" + map.size(), variable.getLowerBound(),
						variable.getUpperBound());
				map.put(variable, v);
			}
			stack.push(v);
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(Operation operation) {
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
			}
			stack.push(new Operation(operation.getOperator(), operands));
		}

	}

}
