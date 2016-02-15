package com.syncedsoftware.iassembly;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import com.syncedsoftware.iassembly.iasm_base.Simulation;
import com.syncedsoftware.iassembly.iasm_base.memory.Memory;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by izodine on 2/4/16.
 */
public class MemoryFragment extends Fragment implements Memory.MemoryListener, Simulation.SimulationReadyListener {

    private ArrayAdapter<String> mAdapter;
    @Bind(R.id.memory_listView) GridView mGridView;
    private SimulationLink mSimulationLink;

    public MemoryFragment(){ }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSimulationLink = (MainActivity)context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_memory, container, false);
        ButterKnife.bind(this, rootView);

        mSimulationLink.addSimulationReadyListener(this);

        String[] stuff = new String[2];
        for(int i = 0; i < 2; i++){
            if(i % 2 == 0){
                stuff[i] = Integer.toString(i/2);
                continue;
            }
            stuff[i] = "Value";
        }
        ArrayList<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(stuff));

        mAdapter = new ArrayAdapter<>(getActivity(), R.layout.memory_view_item, R.id.memoryTextView, list);

        mGridView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onMemoryChanged(byte[] compressedMemory) {
        Log.v(getClass().getSimpleName(), Arrays.toString(compressedMemory));

        mAdapter.clear();
        mAdapter.add(getActivity().getString(R.string.memory_address_label));
        mAdapter.add(getActivity().getString(R.string.memory_value_label));

        for(int i = 0; i < compressedMemory.length; i++){
            mAdapter.add(Integer.toString(i));
            mAdapter.add(Byte.toString(compressedMemory[i]));
        }
    }

    @Override
    public void onMemoryCompressed(Memory memory) {
        byte[] compressedMemory = memory.getCompressedMemory();

        Log.v(getClass().getSimpleName(), Arrays.toString(compressedMemory));

        mAdapter.clear();
        mAdapter.add("Address");
        mAdapter.add("Value");

        for(int i = 0; i < compressedMemory.length; i++){
            mAdapter.add(Integer.toString(i));
            mAdapter.add(Byte.toString(compressedMemory[i]));
        }
    }

    @Override
    public void onSimulationReady(Simulation simulation) {
        simulation.Interpreter.InternalMemory.addListener(new Handler(), this);
    }
}
