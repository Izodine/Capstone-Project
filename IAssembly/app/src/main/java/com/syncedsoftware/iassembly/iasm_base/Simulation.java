package com.syncedsoftware.iassembly.iasm_base;

/**
 * Created by izodine on 1/28/16.
 */

import android.content.Context;
import android.os.Handler;

import com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that wraps all components together needed to create a simulation.
 * It internally creates an Interpreter, Memory, and RegisterManager and manages all of them.
 */
public final class Simulation extends Thread {

    /**
     * Bundle id for register values
     */
    public static final String BUNDLE_REGISTERS = "BUNDLE_REGISTERS";

    /**
     * Bundle id for line errors
     */
    public static final String BUNDLE_LINE_ERR = "BUNDLE_LINE_ERR";

    public static final int EXECUTE_MODE = 0;
    public static final int STEP_MODE = 1;

    public final Interpreter Interpreter = new Interpreter();

    private List<SimulationListener> _callbacks;
    private Handler _handler;

    private int _mode;

    public Simulation(final int mode, List<SimulationListener> callbacks, Handler handler){
        if(mode != EXECUTE_MODE && mode != STEP_MODE)
            throw new IllegalArgumentException("Invalid mode passed to Simulation constructor.");

        this._callbacks = callbacks;
        _mode = mode;
        this._handler = handler;
    }


    @Override
    public void run() {
        super.run();
        if(_mode == EXECUTE_MODE)
            Interpreter.interpret();
        else if(_mode == STEP_MODE)
            Interpreter.interpretStep();
    }

    public void step(){
        Interpreter.step();
    }

    /**
     * Loads a list of commands into the Interpeter. If there is an error loading, it returns false.
     * @param lines
     * @return Returns false if there is an error loading.
     */
    public boolean load(ArrayList<String> lines, Context context){
       return Interpreter.load(lines, _callbacks, _handler, context);
    }

    public interface SimulationReadyListener{
        /**
         * Called when simulation is ready.
         */
        void onSimulationReady(Simulation simulation);
    }


    public interface SimulationListener {

        /**
         * Called when the simulation starts
         */
        void onStartSimulation();

        /**
         * Called when the state of the simulation changes.
         * @param msg A string to display to the user
         */
        void onSendOutput(final String msg);

        /**
         *  Called when there is an error.
         */
        void onSendErrorOutput(final String msg);

        /**
         * Called when the simulation ends.
         */
        void onEndSimulation();
    }
}
