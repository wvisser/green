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
  private int invocations = 0;

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

  @Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

  public Expression propagate(Expression expression,
			Map<Variable, Variable> map) {

    try {
      log.log(Level.FINEST, "Before Canonization: " + expression);
      invocations++;
      PropagationVisitor propagationVisitor = new PropagationVisitor();
			expression.accept(propagationVisitor);
      propagationVisitor.showStack();
			Expression propagated = propagationVisitor.getExpression();
      return propagated;
    } catch (VisitorException x) {
      log.log(Level.SEVERE,"encountered an exception -- this should not be happening!",x);
    }
    return null;
  }

  private static class PropagationVisitor extends Visitor {

    private Stack<Expression> stack;

    private IntVariable var;

    private IntConstant const1;

    public PropagationVisitor() {
			stack = new Stack<Expression>();
		}

    public Expression getExpression() {
			return stack.pop();
		}

    public void showStack() {
      System.out.println("STACK:");
      for (Expression e : stack) {
        System.out.println(e);
      }
      System.out.println("-------");
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
			case ADD:
				nop = Operation.Operator.ADD;
				break;
      case AND:
  			nop = Operation.Operator.AND;
  			break;
			default:
				break;
			}
      if (nop != null) {
        showStack();
        Expression r = stack.pop();
        System.out.println("Popped r = " + r);
        Expression l = stack.pop();
        System.out.println("Popped l = " + l);
        if (nop == Operation.Operator.EQ) {
          if (r instanceof IntVariable && l instanceof IntConstant) {
            var = (IntVariable) r;
            const1 = (IntConstant) l;
          } else if (l instanceof IntVariable && r instanceof IntConstant) {
            var = (IntVariable) l;
            const1 = (IntConstant) r;
          }
          stack.push(new Operation(nop, l, r));
        } else if (nop == Operation.Operator.ADD) {
          System.out.println("1!!!!!!!!");
          if (r instanceof IntVariable) {
            System.out.println("2!!!!!!!!");
            if (((IntVariable) r).getName().equals(var.getName())) {
              System.out.println("PUSH R");
              stack.push(new Operation(nop, l, const1));
            }
          }
          if (l instanceof IntVariable) {
            System.out.println("3!!!!!!!!");
            if (((IntVariable) l).getName().equals(var.getName())) {
              System.out.println("PUSH L");
              stack.push(new Operation(nop, const1, r));
            }
          }
        } else if (nop == Operation.Operator.AND) {
          stack.push(new Operation(nop, l, r));
        }
      } else {
        /*for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}*/
				stack.push(operation);
      }
    }
  }
}
