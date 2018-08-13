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
import za.ac.sun.cs.green.expr.Operation.Operator;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

public class ConstantPropagation extends BasicService {

	/**
	 * Number of times the slicer has been invoked.
	 */
	private int invocations = 0;
	private static boolean varLeft;
	private static Map<Expression, Expression> varMap;

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagateAndSimplify(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	/**
	 * Propagates variables from given equality expressions and simplifies
	 * equation to attempt to reveal variable's values according to each
	 * equation or true/false if {@link za.ac.sun.cs.green.expr.Expression
	 * Expression} is boolean.
	 * 
	 * @param expression
	 *            The expression to propagate and simplify.
	 * @param map
	 *            A variable map.
	 * @return Simplified {@link za.ac.sun.cs.green.expr.Expression Expression}.
	 */
	public Expression propagateAndSimplify(Expression expression, Map<Variable, Variable> map) {
		varMap = new HashMap<Expression, Expression>();
		try {
			Expression finalExpression;
			log.log(Level.FINEST, "Before propagation: " + expression);
			/* Find where variables sit relative to constants */
			PositionVisitor positionVisitor = new PositionVisitor();
			expression.accept(positionVisitor);
			/* Map variables */
			VariableVisitor variableVisitor = new VariableVisitor();
			expression.accept(variableVisitor);
			variableVisitor.processMap();
			/* Propagate variables */
			PropagationVisitor propagationVisitor = new PropagationVisitor();
			expression.accept(propagationVisitor);
			expression = propagationVisitor.getExpression();
			log.log(Level.FINEST, "After propagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
		}
		return null;
	}

	/**
	 * Visits the operations to determine where variables lie in equality
	 * statements, whether it is left or right.
	 */
	private static class PositionVisitor extends Visitor {
		private Stack<Expression> stack;

		public PositionVisitor() {
			stack = new Stack<Expression>();
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			stack.push(operation);
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
				nop = Operation.Operator.LT;
				break;
			case LE:
				nop = Operation.Operator.LE;
				break;
			case GT:
				nop = Operation.Operator.GT;
				break;
			case GE:
				nop = Operation.Operator.GE;
				break;
			default:
				break;
			}

			if (nop != null) {
				Operation oper = (Operation) stack.pop();
				Expression r;
				Expression l;
				for (Expression exp : oper.getOperands()) {
					stack.push(exp);
				}
				r = stack.pop();
				l = stack.pop();
				if (nop.equals(Operation.Operator.EQ)) {
					if (r instanceof IntVariable && l instanceof IntConstant) {
						varLeft = false;
					} else if (l instanceof IntVariable && r instanceof IntConstant) {
						varLeft = true;
					}
				}
			}
		}
	}

	/**
	 * Visits each equality and maps the
	 * {@link za.ac.sun.cs.green.expr.IntVariable IntVariables} to their
	 * assigned {@link za.ac.sun.cs.green.expr.IntConstant IntConstants} to a
	 * variable map.
	 */
	private static class VariableVisitor extends Visitor {
		private Stack<Expression> stack;

		public VariableVisitor() {
			stack = new Stack<Expression>();
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
			stack.push(operation);
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
				nop = Operation.Operator.LT;
				break;
			case LE:
				nop = Operation.Operator.LE;
				break;
			case GT:
				nop = Operation.Operator.GT;
				break;
			case GE:
				nop = Operation.Operator.GE;
				break;
			default:
				break;
			}

			if (nop != null) {
				Operation oper = (Operation) stack.pop();
				Expression r;
				Expression l;
				for (Expression exp : oper.getOperands()) {
					stack.push(exp);
				}
				r = stack.pop();
				l = stack.pop();
				/* Map variables to their assigned constant */
				if (nop.equals(Operation.Operator.EQ)) {
					if (r instanceof IntVariable && l instanceof IntConstant) {
						varMap.put(r, l);
					} else if (l instanceof IntVariable && r instanceof IntConstant) {
						varMap.put(l, r);
					} else if (l instanceof IntVariable && r instanceof IntVariable) {
						if (varLeft) {
							varMap.put(l, r);
						} else {
							varMap.put(r, l);
						}
					}
				}
			}
		}

		/**
		 * Looks for symbolic links between already mapped variables and
		 * replaces them with their respective constants.
		 */
		public void processMap() {
			Map<Expression, Expression> tempMap = new HashMap<Expression, Expression>();
			for (int i = 0; i < varMap.size(); i++) {
				/* Find variables assigned to constants */
				for (Expression keyI : varMap.keySet()) {
					if (varMap.get(keyI) instanceof IntConstant) {
						tempMap.put(keyI, varMap.get(keyI));
					}
				}
				/* Find variables assigned to variables who have constants */
				for (Expression keyI : varMap.keySet()) {
					if (varMap.get(keyI) instanceof IntVariable) {
						if (tempMap.containsKey(varMap.get(keyI))) {
							varMap.put(keyI, (Expression) tempMap.get(varMap.get(keyI)));
						}
					}
				}
			}
		}
	}

	/**
	 * Visits each operation and looks to replace
	 * {@link za.ac.sun.cs.green.expr.IntVariable IntVariable's} with their
	 * assigned {@link za.ac.sun.cs.green.expr.IntConstant IntConstants}.
	 */
	private static class PropagationVisitor extends Visitor {

		private Stack<Expression> stack;

		public PropagationVisitor() {
			stack = new Stack<Expression>();
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
			Operation.Operator nop = null;
			switch (op) {
			case EQ:
				nop = Operation.Operator.EQ;
				break;
			case NE:
				nop = Operation.Operator.NE;
				break;
			case LT:
				nop = Operation.Operator.LT;
				break;
			case LE:
				nop = Operation.Operator.LE;
				break;
			case GT:
				nop = Operation.Operator.GT;
				break;
			case GE:
				nop = Operation.Operator.GE;
				break;
			default:
				break;
			}

			if (nop != null) {
				Expression r = stack.pop();
				Expression l = stack.pop();

				if ((r instanceof IntVariable) && (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(((IntVariable) l).getName()) < 0)) {
					/*
					 * If variable contains a value and it's on the constant
					 * side of equation then replace it with its assigned
					 * constant.
					 */
					if (varMap.containsKey(r) && varLeft) {
						stack.push(new Operation(nop, l, varMap.get(r)));
					} else if (varMap.containsKey(l) && !varLeft) {
						stack.push(new Operation(nop, varMap.get(l), r));
					} else {
						stack.push(new Operation(nop, r, l));
					}
				} else if ((r instanceof IntVariable) && (l instanceof IntVariable)) {
					/*
					 * If variable contains a value and it's on the constant
					 * side of equation then replace it with its assigned
					 * constant.
					 */
					if (varMap.containsKey(r) && varLeft) {
						stack.push(new Operation(nop, l, varMap.get(r)));
					} else if (varMap.containsKey(l) && !varLeft) {
						stack.push(new Operation(nop, varMap.get(l), r));
					} else {
						stack.push(new Operation(nop, r, l));
					}
				} else if ((r instanceof IntVariable) && (l instanceof IntConstant)
						&& !nop.equals(Operation.Operator.EQ)) {
					/* If the variable has constant assigned, replace it here */
					if (varMap.containsKey(r) && !varLeft) {
						stack.push(new Operation(nop, l, varMap.get(r)));
					} else {
						stack.push(new Operation(nop, l, r));
					}
				} else if ((r instanceof IntConstant) && (l instanceof IntVariable)
						&& !nop.equals(Operation.Operator.EQ)) {
					/* If the variable has constant assigned, replace it here */
					if (varMap.containsKey(l) && varLeft) {
						stack.push(new Operation(nop, varMap.get(l), r));
					} else {
						stack.push(new Operation(nop, l, r));
					}
				} else {
					stack.push(new Operation(nop, l, r));
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				/*
				 * If variable contains a value and it's on the constant side of
				 * equation then replace it with its assigned constant
				 */
				if (varMap.containsKey(r)) {
					stack.push(new Operation(op, l, varMap.get(r)));
				} else if (varMap.containsKey(l)) {
					stack.push(new Operation(op, varMap.get(l), r));
				} else {
					stack.push(new Operation(op, l, r));
				}
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}
	}

}