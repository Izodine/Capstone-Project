package com.syncedsoftware.iassembly;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.syncedsoftware.iassembly.iasm_base.Simulation;
import com.syncedsoftware.iassembly.iasm_base.registers.Register;
import com.syncedsoftware.iassembly.iasm_base.registers.RegisterNames;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Anthony M. Santiago on 1/28/16.
 */
public class RegistersFragment extends Fragment implements Register.RegisterListener,
        Simulation.SimulationReadyListener {

    SimulationLink mSimulationLink;
    @Bind(R.id.eax_value_textView) TextView eax;
    @Bind(R.id.ebx_value_textView) TextView ebx;
    @Bind(R.id.ecx_value_textView) TextView ecx;
    @Bind(R.id.edx_value_textView) TextView edx;
    @Bind(R.id.ax_value_textView)  TextView ax;
    @Bind(R.id.bx_value_textView) TextView bx;
    @Bind(R.id.cx_value_textView) TextView cx;
    @Bind(R.id.dx_value_textView) TextView dx;

    public RegistersFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSimulationLink = (MainActivity)context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_registers, container, false);
        ButterKnife.bind(this, rootView);

        mSimulationLink.addSimulationReadyListener(this);

        return rootView;
    }


    @Override
    public void onRegisterChanged(final String regName, final int value) {
        switch (regName.toLowerCase()) {
            case RegisterNames.eax:
                eax.setText(Integer.toString(value));
                ax.setText(Short.toString((short) value));
                break;

            case RegisterNames.ebx:
                ebx.setText(Integer.toString(value));
                bx.setText(Short.toString((short)value));
                break;

            case RegisterNames.ecx:
                ecx.setText(Integer.toString(value));
                cx.setText(Short.toString((short)value));
                break;

            case RegisterNames.edx:
                edx.setText(Integer.toString(value));
                dx.setText(Short.toString((short)value));
                break;

        }

    }

    @Override
    public void onSimulationReady(Simulation simulation) {
        simulation.InternalInterpreter.InternalRegisterManager.addListenerToAll(this, new Handler());
        HashMap<String, Register> registers =
                (HashMap<String, Register>) simulation.InternalInterpreter.InternalRegisterManager.getAllRegisters();

        for(Map.Entry<String, Register> reg: registers.entrySet()){
            reg.getValue().write(0);
        }

    }
}
