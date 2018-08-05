
package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;

public class ConstantPropagationTest {

/*TODO show that code is working*/

    public static Green solver;

    @BeforeClass
		public static void initialize() {
			new OnlyConstantPropogationTest();
            new SimplSimplificationConstantPropogationTest();
		}
}
