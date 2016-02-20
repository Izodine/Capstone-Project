package com.syncedsoftware.iassembly;

import android.os.Handler;

import com.syncedsoftware.iassembly.iasm_base.Simulation;
import com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter;

import java.util.ArrayList;

/**
 * Created by Anthony M. Santiago on 2/10/16.
 */
public interface SimulationLink {

    void addSimulationListener(Interpreter.InterpreterListener listener);
    void removeSimulationListener(Interpreter.InterpreterListener listener);
    void addSimulationReadyListener(Simulation.SimulationReadyListener listener);
    void removeSimulationReadyListener(Simulation.SimulationReadyListener listener);
    void startSimulation(ArrayList<String> lines, int mode, Handler handler);
    void stopSimulation();
    void stepSimulation();
    boolean fail();

}
