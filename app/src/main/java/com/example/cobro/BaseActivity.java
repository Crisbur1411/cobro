package com.example.cobro;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected abstract @IdRes int getBottomNavigationMenuItemId();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBottomNavigation();

    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(getBottomNavigationMenuItemId());

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_inicio && getBottomNavigationMenuItemId() != R.id.nav_inicio) {
                    startActivity(new Intent(this, CobroActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    return true;
                } else if (itemId == R.id.nav_cortes && getBottomNavigationMenuItemId() != R.id.nav_cortes) {
                    startActivity(new Intent(this, CortesActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    return true;
                } else if (itemId == R.id.nav_conexion && getBottomNavigationMenuItemId() != R.id.nav_conexion) {
                    startActivity(new Intent(this, Bluetooth.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    return true;
                }

                return true;
            });
        }
    }
}
