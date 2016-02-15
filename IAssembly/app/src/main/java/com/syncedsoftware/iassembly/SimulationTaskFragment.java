package com.syncedsoftware.iassembly;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;

import com.syncedsoftware.iassembly.iasm_base.Simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by izodine on 1/28/16.
 */
public class SimulationTaskFragment extends Fragment {

    private Simulation mSimulationThread;
    private List<Simulation.SimulationReadyListener> readyListeners = new ArrayList<>();
    private List<Simulation.SimulationListener> simulationListeners = new ArrayList<>();

    //private Simulation.SimulationListener mCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //mCallbacks = (Simulation.SimulationListener)activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //if(context instanceof MainActivity){ mCallbacks = (Simulation.SimulationListener)context; }
    }

    public void addSimulationListener(Simulation.SimulationListener listener){
        simulationListeners.add(listener);
    }

    public void removeSimulationListener(Simulation.SimulationListener listener){
        simulationListeners.remove(listener);
    }

    public void addReadyListener(Simulation.SimulationReadyListener listener){
        readyListeners.add(listener);
    }

    public void removeReadyListener(Simulation.SimulationReadyListener listener){
        readyListeners.remove(listener);
    }

    public void startSimulation(ArrayList<String> lines, int mode, Handler handler){
        mSimulationThread = new Simulation(mode, simulationListeners, handler);

        for(Simulation.SimulationReadyListener listener: readyListeners){
            listener.onSimulationReady(mSimulationThread);
        }

        if(mSimulationThread.load(lines, getContext()))
            mSimulationThread.start();

    }


    public void stopSimulation(){
        if(mSimulationThread != null) {
            mSimulationThread.interrupt();
        }
    }

    public void stepSimulation(){
        mSimulationThread.step();
    }


}
