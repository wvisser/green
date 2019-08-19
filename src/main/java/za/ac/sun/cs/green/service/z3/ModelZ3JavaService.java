package za.ac.sun.cs.green.service.z3;

import com.microsoft.z3.*;
import org.apache.logging.log4j.Level;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.ModelService;
import za.ac.sun.cs.green.util.Reporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ModelZ3JavaService extends ModelService {

	Context ctx;
	Solver Z3solver;
	protected long timeConsumption = 0;
	protected long translationTimeConsumption = 0;
	protected long satTimeConsumption = 0;
	protected long unsatTimeConsumption = 0;
	protected int conjunctCount = 0;
	protected int variableCount = 0;
	private final static String LOGIC = "QF_LIA";

	public ModelZ3JavaService(Green solver, Properties properties) {
		super(solver);
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		try {
			ctx = new Context(cfg);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
		}
	}

	@Override
	protected Map<Variable, Object> model(Instance instance) {
		long startTime = System.currentTimeMillis();
		HashMap<Variable, Object> results = new HashMap<Variable, Object>();
		// translate instance to Z3
		long T0translation = System.currentTimeMillis();
		Z3JavaTranslator translator = new Z3JavaTranslator(ctx);
		try {
			instance.getExpression().accept(translator);
		} catch (VisitorException e1) {
			log.log(Level.WARN, "Error in translation to Z3" + e1.getMessage());
		}
		// get context out of the translator
		BoolExpr expr = translator.getTranslation();
		// model should now be in ctx
		try {
//            System.out.println(expr.toString());
			Z3solver = ctx.mkSolver(LOGIC);
			Z3solver.add(expr);
		} catch (Z3Exception e1) {
			log.log(Level.WARN, "Error in Z3" + e1.getMessage());
		}
		conjunctCount += instance.getExpression().getCachedString().split("&&").length;
		variableCount += translator.getVariableCount();
		translationTimeConsumption += (System.currentTimeMillis() - T0translation);
		//solve
		try { // Real Stuff is still untested
			if (Status.SATISFIABLE == Z3solver.check()) {
				Map<Variable, Expr> variableMap = translator.getVariableMap();
				Model model = Z3solver.getModel();
				for (Map.Entry<Variable, Expr> entry : variableMap.entrySet()) {
					Variable greenVar = entry.getKey();
					Expr z3Var = entry.getValue();
					Expr z3Val = model.evaluate(z3Var, false);
					Object val = null;
					if (z3Val.isIntNum()) {
						val = Integer.parseInt(z3Val.toString());
					} else if (z3Val.isRatNum()) {
						val = Double.parseDouble(z3Val.toString());
					} else {
						log.log(Level.WARN, "Error unsupported type for variable " + z3Val);
						return null;
					}
					results.put(greenVar, val);
					String logMessage = "" + greenVar + " has value " + val;
//					log.log(Level.INFO,logMessage);
				}
			} else {
//				log.log(Level.WARNING,"constraint has no model, it is infeasible");
				long a = System.currentTimeMillis() - startTime;
				timeConsumption += a;
				unsatTimeConsumption += a;
				return null;
			}
		} catch (Z3Exception e) {
			log.log(Level.WARN, "Error in Z3" + e.getMessage());
		}
		long a = System.currentTimeMillis() - startTime;
		timeConsumption += a;
		satTimeConsumption += a;
		return results;
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "cacheHitCount = " + cacheHitCount);
		reporter.report(getClass().getSimpleName(), "cacheMissCount = " + cacheMissCount);
		reporter.report(getClass().getSimpleName(), "satCacheHitCount = " + satHitCount);
		reporter.report(getClass().getSimpleName(), "unsatCacheHitCount = " + unsatHitCount);
		reporter.report(getClass().getSimpleName(), "satCacheMissCount = " + satMissCount);
		reporter.report(getClass().getSimpleName(), "unsatCacheMissCount = " + unsatMissCount);
		reporter.report(getClass().getSimpleName(), "satQueries = " + satCount);
		reporter.report(getClass().getSimpleName(), "unsatQueries = " + unsatCount);
		reporter.report(getClass().getSimpleName(), "timeConsumption = " + timeConsumption);
		reporter.report(getClass().getSimpleName(), "satTimeConsumption = " + satTimeConsumption);
		reporter.report(getClass().getSimpleName(), "unsatTimeConsumption = " + unsatTimeConsumption);
		reporter.report(getClass().getSimpleName(), "storageTimeConsumption = " + storageTimeConsumption);
		reporter.report(getClass().getSimpleName(), "translationTimeConsumption = " + translationTimeConsumption);
		reporter.report(getClass().getSimpleName(), "conjunctCount = " + conjunctCount);
		reporter.report(getClass().getSimpleName(), "variableCount = " + variableCount);
	}

}