//package optimize.gurobi;
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
//import org.matsim.api.core.v01.Id;
//
//import gurobi.GRB;
//import gurobi.GRBEnv;
//import gurobi.GRBException;
//import gurobi.GRBLinExpr;
//import gurobi.GRBModel;
//import gurobi.GRBVar;
//import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
//import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
//import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
//import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
//import playground.dgrether.koehlerstrehlersignal.data.DgGreen;
//import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
//import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
//import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
//
//public class OptimizeMIP {
//	
//	// TODO this class only works for time step size 1 (i.e. offsets that may vary in one second steps)
//
//	private GRBEnv env;
//	private GRBModel model;
//
//	private DgCommodities commodities;
//	private DgKSNetwork ksNet;
//
//	private List<DgCrossing> signalizedCrossings = new LinkedList<>();
//	/** list of all lights, i.e. interior streets of intersections */
//	private List<Id<DgStreet>> lights = new LinkedList<>();
//
//	/** cycle time */
//	private int gamma;
//
//	/** byte matrix for each light in the non-expanded network. for each time step j (0..gamma) it gives 1 if for the choosen offset i (0..gamma) the signal would be green at this time step; 0 otherwise 
//	 * i.e. it is transposed Q with time steps in the expanded network as rows and offsets as columns */
//	private Map<Id<DgStreet>, byte[][]> Qt = new HashMap<>();
//	
//	/** byte array for each light in the expanded network. for each offset i (0..gamma) */
//	private Map<Id<DgStreet>, byte[]> Qj = new HashMap<>();
//	// TODO klassen für expanded network definieren (TtExpandedStreet etc) - kennen alle noch ihre orginal-versionen. ODER: maps machen, mit original-expanded und dafür nur DgStreet etc benutzen mit anderen Ids
//
//	public OptimizeMIP(DgCommodities commodities, DgKSNetwork ksNet) {
//		this.commodities = commodities;
//		this.ksNet = ksNet;
//		try {
//			this.env = new GRBEnv("mip.log");
//			this.model = new GRBModel(env);
//		} catch (GRBException e) {
//			System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
//			e.printStackTrace();
//		}
//		// collect signalized intersections
//		for (DgCrossing crossing : ksNet.getCrossings().values()) {
//			if (!crossing.getLights().isEmpty()) {
//				signalizedCrossings.add(crossing);
//				// set k. only once (i.e. ignore multiple programs)
//				for (DgProgram program : crossing.getPrograms().values()) {
//					gamma = program.getCycle();
//					if (crossing.getPrograms().size() > 1) {
//						System.err.println("A signal system has more than one program. Only the first program will be processed in the optimization!");
//					}
//					break;
//				}
//			}
//			// collect lights, i.e. interior streets of intersections
//			for (Id<DgStreet> light : crossing.getLights().keySet()) {
//				this.lights.add(light);
//			}
//			// TODO fill Q
//			for (DgProgram program : crossing.getPrograms().values()) {
//				for (DgGreen green : program.getGreensByLightId().values()){
//					// TODO change Id in DgGreen to Id<DgStreet> when optimization-changes where copied to another repo
//					Id<DgStreet> lightId = Id.create(green.getLightId(), DgStreet.class);
//					Qt.put(lightId, new byte[gamma][gamma]); // initializes with zeros
//					int onset = (green.getOffset() + program.getOffset())%gamma;
//					int dropping = onset + green.getLength();
//					for (int j= onset; j < dropping; j++){ // time steps
//						for (int i=0; i < gamma; i++){ // offsets
//							Qt.get(lightId)[(j+i)%gamma][i] = 1;
//						}
//					}
//				}
//			}
//		}
//		
//		// TODO expand the network! überlegen ob dann einzelne sachen von oben erst danach gemacht werden sollten
//		// TODO fill Qj
//	}
//
//	public void optimize() {
//		try {
//			// Create variables
//			// parameters of addVar: lower, upper bound, linear obj coefficient (zero means will be set later)
//
//			/* variable f */
//			Map<Id<DgCommodity>, Map<Id<DgStreet>, GRBVar>> f = new HashMap<>();
//			for (DgCommodity theta : commodities.getCommodities().values()) {
//				f.put(theta.getId(), new HashMap<>());
//				for (DgStreet e : ksNet.getStreets().values()) {
//					// f(theta)(e) canot exceed demand nor capacity. It's coefficient for the objective is the travel time t
//					f.get(theta.getId()).put(e.getId(), model.addVar(0.0, Math.min(theta.getFlow(), e.getCapacity()), e.getCost(), GRB.CONTINUOUS, "f_" + theta.getId() + "_" + e.getId()));
//				}
//			}
//			/* variable b */
//			Map<Id<DgCrossing>, GRBVar[]> b = new HashMap<>();
//			for (DgCrossing n : signalizedCrossings) { // TODO nur für zugeh. crossing in unexpanded graph
//				b.put(n.getId(), new GRBVar[gamma]);
//				for (int i = 0; i < gamma; i++) {
//					b.get(n.getId())[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "b_" + n.getId() + "_" + i);
//				}
//			}
//
//			// objective not necessary here, because it was already set above in addVar
//
//			// Add constraints:
//			/* constraint 18 */
//			for (DgStreet e : ksNet.getStreets().values()) {
//				if (!lights.contains(e.getId())) {
//					// street is no light, i.e. no interior street of an intersection, i.e. element of A\E
//					GRBLinExpr expr = new GRBLinExpr();
//					for (Id<DgCommodity> theta : commodities.getCommodities().keySet()) {
//						expr.addTerm(1.0, f.get(theta).get(e.getId()));
//					}
//					model.addConstr(expr, GRB.LESS_EQUAL, e.getCapacity(), "cap_" + e);
//				}
//
//			}
//
//			/* constraint 22 */
//			// go through all lights
//			for (DgCrossing n : signalizedCrossings) {
//				for (DgStreet e : n.getLights().values()) {
//					GRBLinExpr expr = new GRBLinExpr();
//					for (Id<DgCommodity> theta : commodities.getCommodities().keySet()) {
//						expr.addTerm(1.0, f.get(theta).get(e.getId()));
//					}
//					for (int i = 0; i < gamma; i++) {
//						expr.addTerm(-e.getCapacity() * Qj.get(e.getId())[i], b.get(n)[i]); // TODO b gilt für zugeh. crossing in unexpanded graph
//					}
//					model.addConstr(expr, GRB.LESS_EQUAL, 0.0, "cap_" + e);
//				}
//			}
//
//			/* constraint 19 */
//			for (DgCrossing system : ksNet.getCrossings().values()) {
//				for (DgCrossingNode v : system.getNodes().values()) {
//					for (Id<DgCommodity> theta : commodities.getCommodities().keySet()) {
//						GRBLinExpr expr = new GRBLinExpr();
//						// TODO this is computationally inefficient. think of adding in- and outlinks to DgCrossingNode or at least collect them once at the beginning in a separate object
//						for (DgStreet e : ksNet.getStreets().values()) {
//							if (e.getToNode().getId().equals(v.getId())) {
//								// inflow link
//								expr.addTerm(1.0, f.get(theta).get(e.getId()));
//							} else if (e.getFromNode().getId().equals(v.getId())) {
//								// outflow link
//								expr.addTerm(-1.0, f.get(theta).get(e.getId()));
//							}
//						}
//						model.addConstr(expr, GRB.EQUAL, 0.0, "flow_" + v + "_" + theta);
//					}
//				}
//			}
//
//			/* constraint 20 */
//			for (DgCommodity theta : commodities.getCommodities().values()) {
//				GRBLinExpr expr = new GRBLinExpr();
//				// TODO is the id correct? look into converting class
//				Id<DgStreet> e = Id.create(theta.getDrainNodeId() + "-" + theta.getSourceNodeId(), DgStreet.class);
//				expr.addTerm(1.0, f.get(theta.getId()).get(e));
//				model.addConstr(expr, GRB.EQUAL, theta.getFlow(), "demand_" + theta);
//			}
//
//			/* constraint 21 */
//			for (DgCrossing n : signalizedCrossings) { // TODO unexpanded!
//				GRBLinExpr expr = new GRBLinExpr();
//				for (int i = 0; i < gamma; i++) {
//					expr.addTerm(1.0, b.get(n.getId())[i]);
//				}
//				model.addConstr(expr, GRB.EQUAL, 1.0, "offset_" + n);
//			}
//
//			// Optimize model
//			model.optimize();
//
//			for (Id<DgCommodity> theta : commodities.getCommodities().keySet()) {
//				for (DgStreet e : ksNet.getStreets().values()) {
//					System.out.println(f.get(theta).get(e.getId()).get(GRB.StringAttr.VarName) + " " + f.get(theta).get(e.getId()).get(GRB.DoubleAttr.X));
//				}
//			}
//			for (DgCrossing n : signalizedCrossings) { // TODO unexpanded!
//				for (int i = 0; i < gamma; i++) {
//					System.out.println(b.get(n.getId())[i].get(GRB.StringAttr.VarName) + " " + b.get(n.getId())[i].get(GRB.DoubleAttr.X));
//				}
//			}
//			System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));
//
//			// Dispose of model and environment
//			model.dispose();
//			env.dispose();
//		} catch (GRBException e) {
//			System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
//			e.printStackTrace();
//		}
//	}
//
//}
