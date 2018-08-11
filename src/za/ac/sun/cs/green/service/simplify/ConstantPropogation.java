package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
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

/**
 *
 * @author 19770235
 */
public class ConstantPropogation extends BasicService {

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
			final Expression e = constant_propogation(instance.getFullExpression(), map);
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

	public Expression constant_propogation(Expression expression, Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Constant Propogation: " + expression);
			invocations++;
			ConstantPropogationVisitor constantPropogationVisitor = new ConstantPropogationVisitor();
			expression.accept(constantPropogationVisitor);
			expression = constantPropogationVisitor.getExpression();
			log.log(Level.FINEST, "After Constant Propogation/Before Simplification: " + expression);
			SimplificationVisitor simplificationVisitor = new SimplificationVisitor();
			expression.accept(simplificationVisitor);
			expression = simplificationVisitor.getExpression();
			log.log(Level.FINEST, "After Simplification: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					  "encountered an exception -- this should not be happening!",
					  x);
		}
		return null;
	}

	/**
	 * Replaces variables with their assigned values
	 */
	private static class ConstantPropogationVisitor extends Visitor {

		private Stack<Expression> stack;
		private HashMap<IntVariable, IntConstant> variables;
		private HashMap<IntVariable, IntConstant> partials;

		public ConstantPropogationVisitor() {
			this.stack = new Stack<Expression>();
			this.variables = new HashMap<IntVariable, IntConstant>();
			this.partials = new HashMap<IntVariable, IntConstant>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(Constant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(Variable variable) {
			stack.push(variable);
		}

		@Override
		public void preVisit(Operation operation) {
			Operation.Operator op = operation.getOperator();
			Expression r = stack.pop();
			Expression l = stack.pop();
			System.out.println("preprocessing operator " + op);
			// simple assignment
			if (op == Operation.Operator.EQ
					  && (r instanceof IntConstant
					  && l instanceof IntVariable)) {
				System.out.println("adding variable " + l + " to list with value " + r);
				variables.put((IntVariable) l, (IntConstant) r);
			} else if (op == Operation.Operator.EQ
					  && (r instanceof IntVariable
					  && l instanceof IntConstant)) {
				System.out.println("adding variable " + r + " to list with value " + l);
				variables.put((IntVariable) r, (IntConstant) l);
			}
			stack.push(l);
			stack.push(r);
		}

		@Override
		public void postVisit(Operation operation) {
			Operation.Operator op = operation.getOperator();
			Expression r = stack.pop();
			Expression l = stack.pop();
			System.out.println("postprocessing operator " + op);
			// complex assignment (1 +/- x) = 2, 2 = (1 +/- x)
			// adds added/subtracted value to x in variables to prepere for simple assignment
			if (false) {
				if (op == Operation.Operator.ADD
						  && l instanceof IntConstant
						  && r instanceof IntVariable) {
					System.out.println("partially assigning -" + l + " to variable " + r);
					partials.put((IntVariable) r, new IntConstant(((IntConstant) l).getValue() * -1));
					stack.push(r);
					return;
				} else if (op == Operation.Operator.ADD
						  && l instanceof IntVariable
						  && r instanceof IntConstant) {
					System.out.println("partially assigning -" + l + " to variable " + r);
					partials.put((IntVariable) l, new IntConstant(((IntConstant) r).getValue() * -1));
					stack.push(l);
					return;
				}
				if (op == Operation.Operator.SUB
						  && l instanceof IntConstant
						  && r instanceof IntVariable) {
					System.out.println("partially assigning " + l + " to variable " + r);
					partials.put((IntVariable) r, (IntConstant) l);
					stack.push(r);
					return;
				} else if (op == Operation.Operator.ADD
						  && l instanceof IntVariable
						  && r instanceof IntConstant) {
					System.out.println("partially assigning " + l + " to variable " + r);
					partials.put((IntVariable) l, (IntConstant) r);
					stack.push(l);
					return;
				}
			}
			// replacement of variables
			if (l instanceof IntVariable && variables.containsKey((IntVariable) l)) {
				System.out.println("replacing variable " + l + " with value " + variables.get(l));
				l = variables.get((IntVariable) l);
			}
			if (r instanceof IntVariable && variables.containsKey((IntVariable) r)) {
				System.out.println("replacing variable " + r + " with value " + variables.get(r));
				r = variables.get((IntVariable) r);
			}
			stack.push(new Operation(op, l, r));
		}
	}

	/**
	 * Simplifies the equations
	 */
	private static class SimplificationVisitor extends Visitor {

		private Stack<Expression> stack;
		private final Operation o_true = new Operation(Operation.Operator.EQ,
				  new IntConstant(0), new IntConstant(0));
		private final Operation o_false = new Operation(Operation.Operator.EQ,
				  new IntConstant(0), new IntConstant(1));

		public SimplificationVisitor() {
			this.stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(Constant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(Variable variable) {
			stack.push(variable);
		}

		/**
		 * Simplifies operations
		 *
		 * @param operation
		 */
		@Override
		public void postVisit(Operation operation) {
			Operation.Operator op = operation.getOperator();
			Expression r = stack.pop();
			Expression l = stack.pop();
			// Operations on two constants
			if (r instanceof IntConstant && l instanceof IntConstant) {
				switch (op) {
					case LT:
						if (r.compareTo(l) < 0) {
							stack.push(o_true);
						} else {
							stack.push(o_false);
						}
						return;
					case LE:
						if (r.compareTo(l) <= 0) {
							stack.push(o_true);
						} else {
							stack.push(o_false);
						}
						return;
					case GT:
						if (r.compareTo(l) > 0) {
							stack.push(o_true);
						} else {
							stack.push(o_false);
						}
						return;
					case GE:
						if (r.compareTo(l) >= 0) {
							stack.push(o_true);
						} else {
							stack.push(o_false);
						}
						return;
					case EQ:
						if (r.compareTo(l) == 0) {
							stack.push(o_true);
						} else {
							stack.push(o_false);
						}
						return;
					case NE:
						if (r.compareTo(l) != 0) {
							stack.push(o_true);
						} else {
							stack.push(o_false);
						}
						return;
					default:
						break;
				}
			}
			// Operations on two operations
			if (r instanceof Operation && l instanceof Operation) {
				switch (op) {
					case AND:
						if (r.equals(o_true) && l.equals(o_true)) {
							stack.push(o_true);
							return;
						} else if ((r.equals(o_true) && l.equals(o_false))
								  || (r.equals(o_false) && l.equals(o_true))
								  || (r.equals(o_false) && l.equals(o_false))) {
							stack.push(o_false);
							return;
						}
					case OR:
						if (r.equals(o_false) && l.equals(o_false)) {
							stack.push(o_false);
							return;
						} else if ((r.equals(o_true) && l.equals(o_false))
								  || (r.equals(o_false) && l.equals(o_true))
								  || (r.equals(o_true) && l.equals(o_true))) {
							stack.push(o_true);
							return;
						}
					default:
						break;
				}
			}
			stack.push(new Operation(op, l, r));
		}
	}
}
