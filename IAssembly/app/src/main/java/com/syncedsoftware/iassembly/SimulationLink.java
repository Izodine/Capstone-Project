package com.syncedsoftware.iassembly;

import android.os.Handler;
import com.syncedsoftware.iassembly.iasm_base.Simulation;
import java.util.ArrayList;

/**
 * Created by Anthony M. Santiago on 2/10/16.
 */
public interface SimulationLink {

    void addSimulationListener(Simulation.SimulationListener listener);
    void removeSimulationListener(Simulation.SimulationListener listener);
    void addSimulationReadyListener(Simulation.SimulationReadyListener listener);
    void removeSimulationReadyListener(Simulation.SimulationReadyListener listener);
    void startSimulation(ArrayList<String> lines, int mode, Handler handler);
    void stopSimulation();
    void stepSimulation();

}
