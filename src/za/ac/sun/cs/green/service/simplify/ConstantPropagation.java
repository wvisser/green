package za.ac.sun.cs.green.service.simplify;

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

private static class ConstantPropagation extends Visitor {

    private Stack<Expression> stack;

    public OrderingVisitor() {
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
            if ((r instanceof IntVariable)
                    && (l instanceof IntVariable)
                    && (((IntVariable) r).getName().compareTo(
                            ((IntVariable) l).getName()) < 0)) {
                stack.push(new Operation(nop, r, l));
            } else if ((r instanceof IntVariable)
                    && (l instanceof IntConstant)) {
                stack.push(new Operation(nop, r, l));
            } else {
                stack.push(operation);
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

