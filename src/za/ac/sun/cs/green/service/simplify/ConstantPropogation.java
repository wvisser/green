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

import com.sun.org.apache.xpath.internal.operations.Lt;

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
			boolean replacementMade = false;
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
			}
//			
//			for (int i = arity; i > 0; i--) {
//				System.out.println("**" + i + ": " + operands[i - 1]);
//			}
//			System.out.println("sup: " + operands[0].getClass());
//			System.out.println("sup: " + operands[1].getClass());
			
			
			if (operands[0].getClass().equals(IntVariable.class) && operands[1].getClass().equals(IntConstant.class)) {
				Operator operator = operation.getOperator();
				switch (operator) {
				case EQ:
					IntConstant v = map.get(operands[0]);
//					System.out.println("this is bad1: " + operands[0]);
//					System.out.println("this is bad1: " + v);
					if (v == null) {
						IntConstant con = new IntConstant(Integer.parseInt(operands[1].toString()));
						Variable var = new IntVariable(operands[0].toString(), 0, 99999);
						map.put(var, con);
						System.out.println("3: " + var + " : " + con +  "\n");
						//System.out.println("changeMade1");
						changeMade = true;
					} 
					break;
				case NE:
					IntConstant vNE = map.get(operands[0]);
					if (vNE != null) {
						if (vNE.getValue() != (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					} 
					break;
				case LT:
					IntConstant vLT = map.get(operands[0]);
					if (vLT != null) {
						if (vLT.getValue() < (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					} 
					break;
				case LE:
					IntConstant vLe = map.get(operands[0]);
					if (vLe != null) {
						if (vLe.getValue() <= (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					}
					break;
				case GT:
					IntConstant vGT = map.get(operands[0]);
					if (vGT != null) {
						if (vGT.getValue() > (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					} 
					break;
				case GE:
					IntConstant vGE = map.get(operands[0]);
					if (vGE != null) {
						if (vGE.getValue() >= (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					} 
					break;
				default:
					break;
				}
			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntConstant.class)) {
				Operator operator = operation.getOperator();
				switch (operator) {
				case EQ:
					IntConstant v = map.get(operands[1]);
//					System.out.println("this is bad1: " + operands[0]);
//					System.out.println("this is bad1: " + v);
					if (v == null) {
						IntConstant con = new IntConstant(Integer.parseInt(operands[0].toString()));
						Variable var = new IntVariable(operands[1].toString(), 0, 99999);
						map.put(var, con);
						System.out.println("3: " + var + " : " + con +  "\n");
						//System.out.println("changeMade1");
						changeMade = true;
					} 
					break;
				case NE:
					IntConstant vNE = map.get(operands[1]);
					if (vNE != null) {
						if (vNE.getValue() != (new IntConstant(Integer.parseInt(operands[0].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					} 
					break;
				case LT:
					IntConstant vLT = map.get(operands[1]);
					if (vLT != null) {
						if ((new IntConstant(Integer.parseInt(operands[0].toString()))).getValue() < vLT.getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					} 
					break;
				case LE:
					IntConstant vLe = map.get(operands[1]);
					if (vLe != null) {
						if ((new IntConstant(Integer.parseInt(operands[0].toString()))).getValue() <= vLe.getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					}
					break;
				case GT:
					IntConstant vGT = map.get(operands[1]);
					if (vGT != null) {
						if ((new IntConstant(Integer.parseInt(operands[0].toString()))).getValue() > vGT.getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					} 
					break;
				case GE:
					IntConstant vGE = map.get(operands[1]);
					if (vGE != null) {
						if ((new IntConstant(Integer.parseInt(operands[0].toString()))).getValue() >= vGE.getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
						}
					} 
					break;
				default:
					break;
				}
			}
			
//			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntConstant.class) && operation.getOperator().equals(Operation.Operator.EQ)) {
//				IntConstant v = map.get(operands[1]);
//				if (v == null) {
//					IntConstant con = new IntConstant(Integer.parseInt(operands[0].toString()));
//					Variable var = new IntVariable(operands[1].toString(), 0, 99999);
//					map.put(var, con);
//					System.out.println("4: " + var + " : " + con +  "\n");
//					//System.out.println("changeMade2");
//					changeMade = true;
//				}
//			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntVariable.class)) {
				IntConstant v = map.get(operands[0]);
				if (v != null) {
					operands[0] = v;
					//System.out.println("changeMade3");
					changeMade = true;
				}
				v = map.get(operands[1]);
				if (v != null) {
					operands[1] = v;
					//System.out.println("changeMade4");
					changeMade = true;
				}
			}
			
			
//////////// TEST
			
			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(IntConstant.class)) {
				Operator operator = operation.getOperator();
				switch (operator) {
				case EQ:
					Operation opp = (Operation) operands[0];
					switch (opp.getOperator()) {
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
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(1);
							IntVariable var = new IntVariable(opp.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							System.out.println("THIS WAS PRINTED: " + var + " : " + ans);
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(0);
							IntVariable var2 = new IntVariable(opp.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							System.out.println("THIS WAS PRINTED: " + var2 + " : " + ans);
							changeMade = true;
						}
						break;
					case MUL:
						// TODO
						break;
					case DIV:
						// TODO
						break;
					default:
						break;
					}
					break;
				case NE:
					// TODO
					break;
				case LT:
					Operation oppLT = (Operation) operands[0];
					switch (oppLT.getOperator()) {
					case ADD:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.LT, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.LT, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case MUL:
						// TODO
						break;
					case DIV:
						// TODO
						break;
					default:
						break;
					}
					break;
				case LE:
					Operation oppLE = (Operation) operands[0];
					switch (oppLE.getOperator()) {
					case ADD:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(1);
							IntVariable var = new IntVariable(oppLE.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							System.out.println("THIS WAS PRINTED: " + var + " : " + ans);
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(0);
							IntVariable var2 = new IntVariable(oppLE.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							System.out.println("THIS WAS PRINTED: " + var2 + " : " + ans);
							changeMade = true;
						}
						break;
					case MUL:
						// TODO
						break;
					case DIV:
						// TODO
						break;
					default:
						break;
					}
					break;
				case GT:
					Operation oppGT = (Operation) operands[0];
					switch (oppGT.getOperator()) {
					case ADD:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(1);
							IntVariable var = new IntVariable(oppGT.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							System.out.println("THIS WAS PRINTED: " + var + " : " + ans);
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(0);
							IntVariable var2 = new IntVariable(oppGT.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							System.out.println("THIS WAS PRINTED: " + var2 + " : " + ans);
							changeMade = true;
						}
						break;
					case MUL:
						// TODO
						break;
					case DIV:
						// TODO
						break;
					default:
						break;
					}
					break;
				case GE:
					Operation oppGE = (Operation) operands[0];
					switch (oppGE.getOperator()) {
					case ADD:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							// System.out.println(operands[1].getClass().toString() +
							// opp.getOperand(1).getClass());
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(1);
							IntVariable var = new IntVariable(oppGE.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							System.out.println("THIS WAS PRINTED: " + var + " : " + ans);
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(0);
							IntVariable var2 = new IntVariable(oppGE.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							System.out.println("THIS WAS PRINTED: " + var2 + " : " + ans);
							changeMade = true;
						}
						break;
					case MUL:
						// TODO
						break;
					case DIV:
						// TODO
						break;
					default:
						break;
					}
					break;
				case ADD:
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
			
////////////////////
			
//			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(IntConstant.class)
//					&& operation.getOperator().equals(Operation.Operator.EQ)) {
//
//				Operation opp = (Operation) operands[0];
//				switch (opp.getOperator()) {
//				case EQ:
//					break;
//				case NE:
//					// TODO
//					break;
//				case LT:
//					// TODO
//					break;
//				case LE:
//					// TODO
//					break;
//				case GT:
//					// TODO
//					break;
//				case GE:
//					// TODO
//					break;
//				case ADD:
//					if (opp.getOperand(0).getClass().equals(IntConstant.class)
//							&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
//						// System.out.println(operands[1].getClass().toString() +
//						// opp.getOperand(1).getClass());
//						IntConstant const1 = (IntConstant) operands[1];
//						IntConstant const2 = (IntConstant) opp.getOperand(0);
//						int ans = const1.getValue() - const2.getValue();
//						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
//						operands[0] = opp.getOperand(1);
//						operands[1] = new IntConstant(ans);
//						changeMade = true;
//						System.out.println("changeMade5");
//					} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
//							&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
//						IntConstant const1 = (IntConstant) operands[1];
//						IntConstant const2 = (IntConstant) opp.getOperand(1);
//						int ans = const1.getValue() - const2.getValue();
//						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
//						operands[0] = opp.getOperand(0);
//						operands[1] = new IntConstant(ans);
//						System.out.println("changeMade6");
//						changeMade = true;
//					}
//					break;
//				case SUB:
//					// TODO
//					break;
//				case MUL:
//					// TODO
//					break;
//				case NOT:
//					// TODO
//					break;
//				default:
//					break;
//				}
//				System.out.println("11: " + operands[0] + " : " + operands[1] + "\n");
////				System.out.println("changeMade7");
////				changeMade = true;
//			} else if (operands[1].getClass().equals(Operation.class) && operands[0].getClass().equals(IntConstant.class)
//					&& operation.getOperator().equals(Operation.Operator.EQ)) {
//				Operation opp = (Operation) operands[1];
//				switch (opp.getOperator()) {
//				case EQ:
//					// TODO
//					break;
//				case NE:
//					// TODO
//					break;
//				case LT:
//					// TODO
//					break;
//				case LE:
//					// TODO
//					break;
//				case GT:
//					// TODO
//					break;
//				case GE:
//					// TODO
//					break;
//				case ADD:
//					if (opp.getOperand(0).getClass().equals(IntConstant.class)
//							&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
//						// System.out.println(operands[1].getClass().toString() +
//						// opp.getOperand(1).getClass());
//						IntConstant const1 = (IntConstant) operands[0];
//						IntConstant const2 = (IntConstant) opp.getOperand(0);
//						int ans = const1.getValue() - const2.getValue();
//						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
//						operands[0] = opp.getOperand(1);
//						operands[1] = new IntConstant(ans);
//						changeMade = true;
//						//System.out.println("changeMade8");
//					} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
//							&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
//						IntConstant const1 = (IntConstant) operands[0];
//						IntConstant const2 = (IntConstant) opp.getOperand(1);
//						int ans = const1.getValue() - const2.getValue();
//						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
//						operands[0] = opp.getOperand(0);
//						operands[1] = new IntConstant(ans);
//						changeMade = true;
//						//System.out.println("changeMade9");
//					}
//					break;
//				case SUB:
//					// TODO
//					break;
//				case MUL:
//					// TODO
//					break;
//				case NOT:
//					// TODO
//					break;
//				default:
//					break;
//				}
//			}
			
			if (operands[0].getClass().equals(IntConstant.class) && operands[1].getClass().equals(IntConstant.class)) {
				Operator opp = operation.getOperator();
				IntConstant constLeft = (IntConstant) operands[0];
				IntConstant constRight = (IntConstant) operands[1];
				switch (opp) {
				case EQ:
					if ((constLeft.getValue() == constRight.getValue()) && (constLeft.getValue() != 0)) { // if infinite loop, add if the operand != 0 because of the 0==0 case for true
						stack.push(Operation.TRUE);
						replacementMade = true;
					} else if (!operands[0].equals(operands[1])) {
						stack.push(Operation.FALSE);
						replacementMade = true;
					}
					break;
				case LT:
					if(constLeft.getValue() < constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case LE:
					if(constLeft.getValue() <= constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case GT:
					if(constLeft.getValue() > constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case GE:
					if(constLeft.getValue() >= constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case NE:
					if(constLeft.getValue() != constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				default:
					break;
				}
			}
			
			
			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(Operation.class)) {
				if (operands[0].equals(Operation.TRUE) && !operands[1].equals(Operation.TRUE) && operation.getOperator().equals(Operator.AND)) {
					Operation newopp = (Operation) operands[1];
					operands[0] = newopp.getOperand(0);
					operands[1] = newopp.getOperand(1);
					operation.setOperator(newopp.getOperator());
				} else if (operands[1].equals(Operation.TRUE) && !operands[0].equals(Operation.TRUE) && operation.getOperator().equals(Operator.AND)) {
					Operation newopp = (Operation) operands[0];
					operands[0] = newopp.getOperand(0);
					operands[1] = newopp.getOperand(1);
					operation.setOperator(newopp.getOperator());
				}
				
				
				System.out.println("could split");
				Map<Variable, IntConstant> mapLeft = new HashMap<Variable, IntConstant>();
				Map<Variable, IntConstant> mapRight = new HashMap<Variable, IntConstant>();
				Expression leftBefore = operands[0];
				Expression rightBefore = operands[1];
				
				Expression left = GetSimplifiedExpression(mapLeft, operands[0]);
				Expression right = GetSimplifiedExpression(mapRight, operands[1]);			
				System.out.println("leftBefore: " + leftBefore);
				System.out.println("rightBefore: " + rightBefore);
				System.out.println("leftAfter: " + left);
				System.out.println("rightAfter: " + right);
				
//				
//				if (leftBefore.toString().equals(left.toString()) && rightBefore.toString().equals(right.toString())) {
//					changeMade = false;
//					System.out.println("changeMade10");
//				} else {
//					operands[0] = left;
//					operands[1] = right;
//					changeMade = true;
//					System.out.println("changeMade11");
//				}
//				System.out.println("leffft: " + left);
//				System.out.println("righhht: " + right);
			}
			
			
			
			System.out.println("5: " + operation.getOperator() + "\n");
			System.out.println("6: " + operands[0] + "\n");
			System.out.println("7: " + operands[1] + "\n");
			if (replacementMade == false) {
				stack.push(new Operation(operation.getOperator(), operands));
			}
			
			
			
		}

	}
}
