package com.accessible.team2.strollingaround;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Eric on 2/6/2016.
 */
public class DrawerAdapter extends ArrayAdapter<Markers> {
    //class to populate and create drawer

    private final Context context;
    private final List<Markers> alMarkers;

    public DrawerAdapter(Context context, List<Markers> alMarkers) {
        super(context, 0, alMarkers);
        this.context = context;
        this.alMarkers = alMarkers;
        //debugging log data
        for (int i = 0; i < alMarkers.size(); i++) {
            Log.i("MENU", "" + alMarkers.get(i).title + " " + alMarkers.get(i).imgId);
        }
    }//end DrawerAdapter constructor

    @Override
    public int getCount() {
        return alMarkers.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View view = convertView;
        //populate drawer
        if (view == null){
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(R.layout.items, parent, false);
            holder = new ViewHolder();
            holder.img = (ImageView) view.findViewById(R.id.img_view);
            holder.title = (TextView) view.findViewById(R.id.text_view);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }

        //debugging log data
        Log.e("MENU", "" + alMarkers.get(position).title + " Position: " + position);
        for (int i = 0; i < alMarkers.size(); i++) {
            Log.e("MENU", "" + alMarkers.get(position).title + " " + alMarkers.get(position).imgId);
        }
        //end log
        holder.img.setImageResource(alMarkers.get(position).imgId);
        holder.title.setText(alMarkers.get(position).title);
        return view;
    }//end getView


    private static class ViewHolder{
        private TextView title;
        private ImageView img;
    }//end ViewHolder class
}//end DrawerAdapter Class
