package com.syncedsoftware.iassembly;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;

import com.syncedsoftware.iassembly.iasm_base.Simulation;
import com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anthony M. Santiago on 1/28/16.
 */
public class SimulationTaskFragment extends Fragment {

    private Simulation mSimulationThread;
    private List<Simulation.SimulationReadyListener> readyListeners = new ArrayList<>();
    private List<Interpreter.InterpreterListener> interpreterListeners = new ArrayList<>();

    //private Simulation.InterpreterListener mCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //mCallbacks = (Simulation.InterpreterListener)activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //if(context instanceof MainActivity){ mCallbacks = (Simulation.InterpreterListener)context; }
    }

    public void addSimulationListener(Interpreter.InterpreterListener listener){
        interpreterListeners.add(listener);
    }

    public void removeSimulationListener(Interpreter.InterpreterListener listener){
        interpreterListeners.remove(listener);
    }

    public void addReadyListener(Simulation.SimulationReadyListener listener){
        readyListeners.add(listener);
    }

    public void removeReadyListener(Simulation.SimulationReadyListener listener){
        readyListeners.remove(listener);
    }

    public void startSimulation(ArrayList<String> lines, int mode, Handler handler){
        mSimulationThread = new Simulation(mode, interpreterListeners, handler);

        for(Simulation.SimulationReadyListener listener: readyListeners){
            listener.onSimulationReady(mSimulationThread);
        }

        if(mSimulationThread.load(lines, getContext()))
            mSimulationThread.start();

    }

    /**
     * Returns weather the simulation failed or not. If the Simulation thread is null,
     * then it returns true.
     * @return True if the simulation failed, false otherwise.
     */
    public boolean fail() {
        return mSimulationThread == null || mSimulationThread.InternalInterpreter.fail();
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
