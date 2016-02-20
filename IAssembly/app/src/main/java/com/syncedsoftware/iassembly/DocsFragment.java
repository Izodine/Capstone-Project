package com.syncedsoftware.iassembly;

/**
 * Created by Anthony M. Santiago on 1/26/16.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class DocsFragment extends Fragment{
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null) {
            ((MainActivity)getActivity()).initTutorial(data.getIntExtra(DocActivity.EXTRAS_TUTORIAL_ID, -1));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_docs, container, false);

        ArrayList<String> list = new ArrayList<>();
        list.add(getString(R.string.docs_title_lesson_0));
        list.add(getString(R.string.docs_title_lesson_1));
        list.add(getString(R.string.docs_title_lesson_2));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),R.layout.doc_view_item, R.id.doc_title_textView, list);

        ListView listView =((ListView)rootView.findViewById(R.id.docs_listView));
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), DocActivity.class);
                intent.putExtra("index", position);
                startActivityForResult(intent, DocActivity.GET_TUTORIAL_LINK);
            }
        });
        listView.setAdapter(adapter);

        return rootView;
    }


}
