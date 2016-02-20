package com.syncedsoftware.iassembly.iasm_base;

/**
 * Created by Anthony M. Santiago on 1/28/16.
 */

import android.content.Context;
import android.os.Handler;
import com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter;
import static com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter.CALLBACKS_ONENDSIMULATION;
import static com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter.CALLBACKS_ONSENDERROROUTPUT;
import static com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter.CALLBACKS_ONSENDOUTPUT;
import static com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter.CALLBACKS_ONSTARTSIMULATION;
import static com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter.InterpreterListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that wraps all components together needed to create a simulation.
 * It internally creates an InternalInterpreter, Memory, and RegisterManager and manages all of them.
 */
public final class Simulation extends Thread implements InterpreterListener{

    public static final int EXECUTE_MODE = 0;
    public static final int STEP_MODE = 1;

    public final Interpreter InternalInterpreter = new com.syncedsoftware.iassembly.iasm_base.interpreter.Interpreter();

    private List<InterpreterListener> _callbacks;
    private Handler _handler;

    private int _mode;

    public Simulation(final int mode, List<InterpreterListener> callbacks, Handler handler){
        if(mode != EXECUTE_MODE && mode != STEP_MODE)
            throw new IllegalArgumentException("Invalid mode passed to Simulation constructor.");

        InternalInterpreter.addCallback(this);
        this._callbacks = callbacks;
        _mode = mode;
        this._handler = handler;
    }

    public boolean failed(){
        return InternalInterpreter.fail();
    }


    @Override
    public void run() {
        super.run();
        if(_mode == EXECUTE_MODE)
            InternalInterpreter.interpret();
        else if(_mode == STEP_MODE)
            InternalInterpreter.interpretStep();
    }

    public void step(){
        InternalInterpreter.step();
    }

    /**
     * Loads a list of executable statements into the Interpeter. If there is an error loading, it returns false.
     * @param lines
     * @param context
     * @return Returns false if there is an error loading.
     */
    public boolean load(ArrayList<String> lines, Context context){
       return InternalInterpreter.load(lines, context);
    }

    public void postToUiThread(final int callbackId, final String msg){
        if(_handler != null && _callbacks != null) {
            _handler.post(new Runnable() {
                @Override
                public void run() {

                    switch (callbackId) {

                        case Interpreter.CALLBACKS_ONSTARTSIMULATION:
                            for (InterpreterListener listener : _callbacks)
                                listener.onStartSimulation();
                            break;

                        case Interpreter.CALLBACKS_ONSENDOUTPUT:
                            for (InterpreterListener listener : _callbacks)
                                listener.onSendOutput(msg);
                            break;

                        case Interpreter.CALLBACKS_ONSENDERROROUTPUT:
                            for (InterpreterListener listener : _callbacks)
                                listener.onSendErrorOutput(msg);
                            break;

                        case Interpreter.CALLBACKS_ONENDSIMULATION:
                            for (InterpreterListener listener : _callbacks)
                                listener.onEndSimulation();
                            break;
                    }

                }
            });
        }
    }

    @Override
    public void onStartSimulation() {
        postToUiThread(CALLBACKS_ONSTARTSIMULATION, null);
    }

    @Override
    public void onSendOutput(String msg) {
        postToUiThread(CALLBACKS_ONSENDOUTPUT,msg);
    }

    @Override
    public void onSendErrorOutput(String msg) {
        postToUiThread(CALLBACKS_ONSENDERROROUTPUT, msg);
    }

    @Override
    public void onEndSimulation() {
        postToUiThread(CALLBACKS_ONENDSIMULATION, null);
    }

    public interface SimulationReadyListener{
        /**
         * Called when simulation is ready.
         */
        void onSimulationReady(Simulation simulation);
    }

}
