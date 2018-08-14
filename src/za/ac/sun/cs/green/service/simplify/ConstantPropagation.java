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

public class ConstantPropagation  extends BasicService {

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
		public Set<Instance> processRequest(Instance instance) {
			@SuppressWarnings("unchecked")
			Set<Instance> result = (Set<Instance>) instance.getData(getClass());
			if (result == null) {
				final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
				final Expression e = propagate(instance.getFullExpression(), map);
				final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
				result = Collections.singleton(i);
				instance.setData(getClass(), result);
			}
			return result;
		}

	public Expression propagate(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Simplification:\n" + expression);
			
			CollectionVisitor collectionVisitor = new CollectionVisitor();
			expression.accept(collectionVisitor);
			Expression propagated = collectionVisitor.getExpression();

			//PropagationVisitor propagationVisitor = new PropagationVisitor();
			//expression.accept(propagationVisitor);
			//Expression propagated = propagationVisitor.getExpression();

			log.log(Level.FINEST, "After Simplification:\n" + propagated);
			return propagated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- the sky is falling!",
					x);
		}
		return null;
	}

	private static class PropagationVisitor extends Visitor {

		private Stack<Expression> stack;

		private SortedSet<Expression> conjuncts;

		private SortedSet<IntVariable> variableSet;

		private boolean unsatisfiable;

		private boolean linearInteger;

		public PropagationVisitor() {
			stack = new Stack<Expression>();
			conjuncts = new TreeSet<Expression>();
			variableSet = new TreeSet<IntVariable>();
			unsatisfiable = false;
			linearInteger = true;
		}

		public SortedSet<IntVariable> getVariableSet() {
			return variableSet;
		}

		public Expression getExpression() {

			

		}

	}

	private static class CollectionVisitor extends Visitor {

		private Stack<Expression> stack;
		
		private Map<Variable, Constant> map = new HashMap<Variable, Constant>();

		public CollectionVisitor() {
			stack = new Stack<Expression>();
			map = new HashMap<Variable, Constant>();
		}

		public HashMap getVariables() {
			return map;
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
			public void postVisit(IntConstant constant) {
				stack.push(constant);
				System.out.println( "Collection Visitor pushed \" " + constant + " \" (constant) to stack.");
			}

		@Override
			public void postVisit(IntVariable variable) {
				stack.push(variable);
				System.out.println("Collection Visitor pushed \" " + variable + " \" (variable) to stack.");
			}

		@Override
			public void postVisit(Operation operation) throws VisitorException {
				Operation.Operator op = operation.getOperator();
				Operation.Operator nop = null;
				switch(op) {
					case EQ:
						nop = Operation.Operator.EQ;
						break;
					case NE:
						nop = Operation.Operator.NE;
						break;
					case GT:
						nop = Operation.Operator.GT;
						break;
					case GE:
						nop = Operation.Operator.GE;
						break;
					case LT:
						nop = Operation.Operator.LT;
						break;
					case LE:
						nop = Operation.Operator.LE;
						break;
					default:
						System.out.println("Default reached switching on operator -- get outa Dodge!");
						break;
				}
				if (nop.equals(Operation.Operator.EQ)) {
					Expression r = stack.pop();
					Expression l = stack.pop();
					if ((r instanceof Constant) && (l instanceof Variable)) {
						map.put((Variable) l, (Constant) r);
						System.out.println(l + " == "+ r + ": Variable \"" + l + "\" added to map.");
						stack.push(new Operation(op, l, r));
						System.out.println("Collection Visitor pushed \"" + l + " " + op + " " + r +"\" (operation) to stack.");
					} else if ((r instanceof Variable) && (l instanceof Constant)) {
						map.put((Variable) r, (Constant) l);
						System.out.println(l + " == "+ r + ": Variable \"" + r + "\" added to map.");
						stack.push(new Operation(op, l, r));
						System.out.println("Collection Visitor pushed \"" + l + " " + op + " " + r +"\" (operation) to stack.");
					} else {
						System.out.println("Operator was EQ, but allas, no variables could be added to map. Maby next pass.");
						stack.push(operation);
						System.out.println("Collection Visitor pushed \"" + operation +"\" (operation) to stack.");
					}	
				} /**/else if ((nop != null) && !(nop.equals(Operation.Operator.EQ))) {
					Expression r = stack.pop();
					Expression l = stack.pop();
					if ((r instanceof IntVariable) && (l instanceof IntVariable) && (((IntVariable) r).getName().compareTo(((IntVariable) l).getName()) < 0)) {
						stack.push(new Operation(nop, r, l));
					} else if ((r instanceof IntVariable)	&& (l instanceof IntConstant)) {
						stack.push(new Operation(nop, r, l));
					} else {
						stack.push(operation);
					}
				}	else if (op.getArity() == 2){
					Expression r = stack.pop();
					Expression l = stack.pop();
					stack.push(new Operation(op, l, r));
					System.out.println("Collection Visitor pushed \"" + l + " " + op + " " + r +"\" (operation) to stack.");
				} else {
					for (int i = op.getArity(); i > 0; i--) {
						stack.pop();
					}
					stack.push(operation);
					System.out.println("Collection Visitor pushed \"" + operation +"\" (operation) to stack.");
				}
			}
	}
}
