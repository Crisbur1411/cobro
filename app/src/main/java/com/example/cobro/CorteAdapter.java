package com.example.cobro;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class CorteAdapter extends ArrayAdapter<CorteTotal> {

    public CorteAdapter(Context context, List<CorteTotal> cortes) {
        super(context, 0, cortes);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        CorteTotal corte = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_corte_total, parent, false);
        }

        TextView textNombre = convertView.findViewById(R.id.textNombre);
        TextView textInfo = convertView.findViewById(R.id.textInfo);

        textNombre.setText(corte.nombre);
        textInfo.setText(corte.info);

        // Si es un mensaje de "sin ventas", personaliza distinto
        if (corte.nombre.equals("Sin ventas")) {
            textNombre.setTextColor(Color.GRAY);
            textNombre.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textInfo.setVisibility(View.GONE);
        } else {
            textInfo.setVisibility(View.VISIBLE);

            // Color del nombre según status
            if (corte.status == 1) {
                textNombre.setTextColor(Color.RED);
            } else if (corte.status == 2) {
                textNombre.setTextColor(Color.parseColor("#388E3C")); // verde oscuro
            } else if (corte.status == 3) {
                textNombre.setTextColor(Color.RED);
            } else if (corte.status == 0) {
                textNombre.setTextColor(Color.BLACK);
            } else {
                textNombre.setTextColor(Color.BLACK);
            }

            textNombre.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }

        return convertView;
    }
}


