package com.syncedsoftware.iassembly.iasm_base.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.syncedsoftware.iassembly.MainActivity;
import com.syncedsoftware.iassembly.R;

import java.util.Random;

/**
 * Created by Anthony M. Santiago on 2/11/16.
 */
public class FactWidgetProvider extends AppWidgetProvider {


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fact_widget_layout);
            views.setOnClickPendingIntent(R.id.widget,pendingIntent);
            views.setTextViewText(R.id.fact_widget_textView, getFact(context));

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private String getFact(Context context){
        int index = (int) (Math.random() * 6);

        switch(index){

            case 0:
                return context.getString(R.string.fact_0);

            case 1:
                return context.getString(R.string.fact_1);

            case 2:
                return context.getString(R.string.fact_2);

            case 3:
                return context.getString(R.string.fact_3);

            case 4:
                return context.getString(R.string.fact_4);

            case 5:
                return context.getString(R.string.fact_5);

            case 6:
                return context.getString(R.string.fact_6);

        }
        return context.getString(R.string.fact_0);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
}
