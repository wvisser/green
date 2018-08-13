


public class ConstantPropagation {

  static void propagate(Operator operator, Expression[] operands) {
    if (operator.equals(Operation.Operator.AND)) {
      for (Expression e : operands) {
        System.out.println(e);
      }
    }
  }

}
