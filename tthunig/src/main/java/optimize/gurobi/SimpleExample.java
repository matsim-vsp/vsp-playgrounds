///*
// *  *********************************************************************** *
// *  * project: org.matsim.*
// *  * DefaultControlerModules.java
// *  *                                                                         *
// *  * *********************************************************************** *
// *  *                                                                         *
// *  * copyright       : (C) 2014 by the members listed in the COPYING, *
// *  *                   LICENSE and WARRANTY file.                            *
// *  * email           : info at matsim dot org                                *
// *  *                                                                         *
// *  * *********************************************************************** *
// *  *                                                                         *
// *  *   This program is free software; you can redistribute it and/or modify  *
// *  *   it under the terms of the GNU General Public License as published by  *
// *  *   the Free Software Foundation; either version 2 of the License, or     *
// *  *   (at your option) any later version.                                   *
// *  *   See also COPYING, LICENSE and WARRANTY file                           *
// *  *                                                                         *
// *  * ***********************************************************************
// */
//package optimize.gurobi;
//
//import gurobi.GRB;
//import gurobi.GRBEnv;
//import gurobi.GRBException;
//import gurobi.GRBLinExpr;
//import gurobi.GRBModel;
//import gurobi.GRBVar;
//import gurobi.GRB.DoubleAttr;
//import gurobi.GRB.StringAttr;
//
///**
// * @author tthunig
// *
// */
//public class SimpleExample {
//
//	public static void main(String[] args) {
//	    try {
//	      GRBEnv    env   = new GRBEnv("mip1.log");
//	      GRBModel  model = new GRBModel(env);
//
//	      // Create variables
//
//	      GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
//	      GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
//	      GRBVar z = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z");
//
//	      // Set objective: maximize x + y + 2 z
//
//	      GRBLinExpr expr = new GRBLinExpr();
//	      expr.addTerm(1.0, x); expr.addTerm(1.0, y); expr.addTerm(2.0, z);
//	      model.setObjective(expr, GRB.MAXIMIZE);
//
//	      // Add constraint: x + 2 y + 3 z <= 4
//
//	      expr = new GRBLinExpr();
//	      expr.addTerm(1.0, x); expr.addTerm(2.0, y); expr.addTerm(3.0, z);
//	      model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");
//
//	      // Add constraint: x + y >= 1
//
//	      expr = new GRBLinExpr();
//	      expr.addTerm(1.0, x); expr.addTerm(1.0, y);
//	      model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");
//
//	      // Optimize model
//
//	      model.optimize();
//
//	      System.out.println(x.get(GRB.StringAttr.VarName)
//	                         + " " +x.get(GRB.DoubleAttr.X));
//	      System.out.println(y.get(GRB.StringAttr.VarName)
//	                         + " " +y.get(GRB.DoubleAttr.X));
//	      System.out.println(z.get(GRB.StringAttr.VarName)
//	                         + " " +z.get(GRB.DoubleAttr.X));
//
//	      System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));
//
//	      // Dispose of model and environment
//
//	      model.dispose();
//	      env.dispose();
//
//	    } catch (GRBException e) {
//	      System.out.println("Error code: " + e.getErrorCode() + ". " +
//	                         e.getMessage());
//	    }
//	  }
//	
//}
