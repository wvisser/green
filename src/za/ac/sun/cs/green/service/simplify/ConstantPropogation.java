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
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

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
			final Map<Variable, IntConstant> map = new HashMap<Variable, IntConstant>();
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
			Map<Variable, IntConstant> map) {
		try {
			log.log(Level.FINEST, "Before Renaming: " + expression);
			invocations++;
			Expression canonized = expression;
			System.out.println("JJJJJJJJJJJJJJJJJJJJJJJJJJJ");
			canonized = GetSimplifiedExpression(map, canonized);
			log.log(Level.FINEST, "After Renaming: " + canonized);
			return canonized;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}
	
	private static Expression GetSimplifiedExpression(Map<Variable, IntConstant> map, Expression ex) throws VisitorException {
		boolean changeMade = false;
		if (ex != null) {
			System.out.println("--run renamer''\n");
			Renamer re = new Renamer(map, changeMade);
			ex = re.rename(ex);
			changeMade = re.getChangeMade();
			while (changeMade == true) {
				changeMade = false;
				System.out.println("--run renamer''\n");
				Renamer re2 = new Renamer(map, changeMade);
				ex = re2.rename(ex);
				changeMade = re2.getChangeMade();
			}
		}
		return ex;
	}
	
	private static class Renamer extends Visitor {
		
		private Map<Variable, IntConstant> map;
		private boolean changeMade;

		private Stack<Expression> stack;

		public Renamer(Map<Variable, IntConstant> map, boolean changeMade) {
			System.out.println("thisagain");
			this.changeMade = changeMade;
			this.map = map;
			stack = new Stack<Expression>();
		}

		public Expression rename(Expression expression) throws VisitorException {
			expression.accept(this);
			return stack.pop();
		}
		
		public boolean getChangeMade() {
			return changeMade;
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
			System.out.println("1: " + variable + "\n");
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
			System.out.println("2: " + constant + "\n");
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
			}
			
			for (int i = arity; i > 0; i--) {
				System.out.println("**" + i + ": " + operands[i - 1]);
			}
//			System.out.println("sup: " + operands[0].getClass());
//			System.out.println("sup: " + operands[1].getClass());
			
			
			if (operands[0].getClass().equals(IntVariable.class) && operands[1].getClass().equals(IntConstant.class) && operation.getOperator().equals(Operation.Operator.EQ)) {
				IntConstant v = map.get(operands[0]);
				System.out.println("this is bad1: " + operands[0]);
				System.out.println("this is bad1: " + v);
				if (v == null) {
					IntConstant con = new IntConstant(Integer.parseInt(operands[1].toString()));
					Variable var = new IntVariable(operands[0].toString(), 0, 99999);
					map.put(var, con);
					System.out.println("3: " + var + " : " + con +  "\n");
					changeMade = true;
				}
			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntConstant.class) && operation.getOperator().equals(Operation.Operator.EQ)) {
				IntConstant v = map.get(operands[1]);
				if (v == null) {
					IntConstant con = new IntConstant(Integer.parseInt(operands[0].toString()));
					Variable var = new IntVariable(operands[1].toString(), 0, 99999);
					map.put(var, con);
					System.out.println("4: " + var + " : " + con +  "\n");
					changeMade = true;
				}
			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntVariable.class)) {
				IntConstant v = map.get(operands[0]);
				if (v != null) {
					operands[0] = v;
					changeMade = true;
				}
				v = map.get(operands[1]);
				if (v != null) {
					operands[1] = v;
					changeMade = true;
				}
			}

			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(IntConstant.class)
					&& operation.getOperator().equals(Operation.Operator.EQ)) {

				Operation opp = (Operation) operands[0];
				switch (opp.getOperator()) {
				case EQ:
					// TODO
//						if (opp.getOperand(1).equals(operands[1])) {
//							operation = new Operation(Operation.Operator.EQ, opp.getOperand(0), opp.getOperand(1));
//							operands[0] = opp.getOperand(0);
//							operands[1] = opp.getOperand(1);
//						} else if (!opp.getOperand(1).equals(operands[1])) {
//							operation = new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1));
//							operands[0] = new IntConstant(0);
//							operands[1] = new IntConstant(1);
//						}
					break;
				case NE:
					// TODO
					break;
				case LT:
					// TODO
					break;
				case LE:
					// TODO
					break;
				case GT:
					// TODO
					break;
				case GE:
					// TODO
					break;
				case ADD:
					if (opp.getOperand(0).getClass().equals(IntConstant.class)
							&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
						// System.out.println(operands[1].getClass().toString() +
						// opp.getOperand(1).getClass());
						IntConstant const1 = (IntConstant) operands[1];
						IntConstant const2 = (IntConstant) opp.getOperand(0);
						int ans = const1.getValue() - const2.getValue();
						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
						operands[0] = opp.getOperand(1);
						operands[1] = new IntConstant(ans);
						changeMade = true;
					} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
							&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
						IntConstant const1 = (IntConstant) operands[1];
						IntConstant const2 = (IntConstant) opp.getOperand(1);
						int ans = const1.getValue() - const2.getValue();
						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
						operands[0] = opp.getOperand(0);
						operands[1] = new IntConstant(ans);
						changeMade = true;
					}
					break;
				case SUB:
					// TODO
					break;
				case MUL:
					// TODO
					break;
				case NOT:
					// TODO
					break;
				default:
					break;
				}
				System.out.println("11: " + operands[0] + " : " + operands[1] + "\n");
				changeMade = true;
			} else if (operands[1].getClass().equals(Operation.class) && operands[0].getClass().equals(IntConstant.class)
					&& operation.getOperator().equals(Operation.Operator.EQ)) {
				Operation opp = (Operation) operands[1];
				switch (opp.getOperator()) {
				case EQ:
					// TODO
					break;
				case NE:
					// TODO
					break;
				case LT:
					// TODO
					break;
				case LE:
					// TODO
					break;
				case GT:
					// TODO
					break;
				case GE:
					// TODO
					break;
				case ADD:
					if (opp.getOperand(0).getClass().equals(IntConstant.class)
							&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
						// System.out.println(operands[1].getClass().toString() +
						// opp.getOperand(1).getClass());
						IntConstant const1 = (IntConstant) operands[0];
						IntConstant const2 = (IntConstant) opp.getOperand(0);
						int ans = const1.getValue() - const2.getValue();
						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
						operands[0] = opp.getOperand(1);
						operands[1] = new IntConstant(ans);
						changeMade = true;
					} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
							&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
						IntConstant const1 = (IntConstant) operands[0];
						IntConstant const2 = (IntConstant) opp.getOperand(1);
						int ans = const1.getValue() - const2.getValue();
						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
						operands[0] = opp.getOperand(0);
						operands[1] = new IntConstant(ans);
						changeMade = true;
					}
					break;
				case SUB:
					// TODO
					break;
				case MUL:
					// TODO
					break;
				case NOT:
					// TODO
					break;
				default:
					break;
				}
			}
			
			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(Operation.class)) {
				Map<Variable, IntConstant> mapLeft = new HashMap<Variable, IntConstant>();
				Map<Variable, IntConstant> mapRight = new HashMap<Variable, IntConstant>();
				Expression leftBefore = operands[0];
				Expression rightBefore = operands[1];
				
				System.out.println("HHHHHHHHHHHH");
				Expression left = GetSimplifiedExpression(mapLeft, operands[0]);
				Expression right = GetSimplifiedExpression(mapRight, operands[1]);
//				Expression left = new Renamer(mapLeft).rename(operands[0]);
//				Expression right = new Renamer(mapRight).rename(operands[1]);
				
				if (leftBefore.toString().equals(left.toString()) && rightBefore.toString().equals(right.toString())) {
					changeMade = false;
				}
				
				System.out.println("leffft: " + left);
				System.out.println("righhht: " + right);
			}
			
			
			
			System.out.println("5: " + operation.getOperator() + "\n");
			System.out.println("6: " + operands[0] + "\n");
			System.out.println("7: " + operands[1] + "\n");
			stack.push(new Operation(operation.getOperator(), operands));
		}

	}
}
