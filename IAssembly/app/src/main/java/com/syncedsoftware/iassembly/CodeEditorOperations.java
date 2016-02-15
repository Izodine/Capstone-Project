package com.syncedsoftware.iassembly;

import android.text.Editable;

/**
 * Created by Anthony M. Santiago on 2/11/16.
 */
public interface CodeEditorOperations {
    Editable getEditorText();
    void setEditorText(String text);
    void notifyStepIsOver();
}
