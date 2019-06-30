package com.ohanacodedev.ocscheduler;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class patientsListAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;

    public patientsListAdapter(Context context, ArrayList<String> values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.termini_text, parent, false);
        TextView textView = rowView.findViewById(R.id.label);
        textView.setText(values.get(position));

        if (position % 2 == 1) {
            rowView.setBackgroundColor(Color.parseColor(GlobalVars.colorBg2));
            textView.setTextColor(Color.parseColor(GlobalVars.colorText3));
        } else {
            rowView.setBackgroundColor(Color.parseColor(GlobalVars.colorBg3));
            textView.setTextColor(Color.parseColor(GlobalVars.colorText4));
        }

        return rowView;
    }
}
