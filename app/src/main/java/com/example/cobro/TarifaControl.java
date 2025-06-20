package com.example.cobro;

public class TarifaControl {
    public double precioMinimo;
    public double precioMaximo;
    public double precioActual;

    public TarifaControl(double minimo, double maximo, double actual) {
        this.precioMinimo = minimo;
        this.precioMaximo = maximo;
        this.precioActual = actual;
    }
}
