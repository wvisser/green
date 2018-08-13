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
      log.log(Level.FINEST, "Before Propagation: " + expression);
      invocations++;
      OrderingVisitor orderingVisitor = new OrderingVisitor();
      expression.accept(orderingVisitor);
      expression = orderingVisitor.getExpression();
      log.log(Level.FINEST, "After Propagation: " + expression);
      return expression;
    } catch (VisitorException x) {
      log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
    }
    return null;
  }

  private static class OrderingVisitor extends Visitor {

    private Stack<Expression> stack;
    private static Map<String, Integer> map;


    public OrderingVisitor() {
      stack = new Stack<Expression>();
      map = new HashMap<>();
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

        if (op == Operation.Operator.EQ) {
          if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
            map.put(r.toString(), Integer.parseInt(l.toString()));
          } else if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
            map.put(l.toString(), Integer.parseInt(r.toString()));
          }
          stack.push(new Operation(nop, l, r));
        } else {
          if (map.containsKey(l.toString())) {
            l = new IntConstant(map.get(l.toString()));
          }
          if (map.containsKey(r.toString())) {
            r = new IntConstant(map.get(r.toString()));
          }
          stack.push(new Operation(nop, l, r));
        }

      } else if (op.getArity() == 2) {
        Expression r = stack.pop();
        Expression l = stack.pop();

        if (map.containsKey(l.toString())) {
          l = new IntConstant(map.get(l.toString()));
        }
        if (map.containsKey(r.toString())) {
          r = new IntConstant(map.get(r.toString()));
        }
        stack.push(new Operation(op, l, r));
      } else {
        for (int i = op.getArity(); i > 0; i--) {
          stack.pop();
        }
        stack.push(operation);
      }
    }

  }

}
