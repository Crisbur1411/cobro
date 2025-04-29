//Modelo de datos el cual utiliza a CorteAdapter para llenar la lista con la informacion requerida
package com.example.cobro;

public class CorteTotal {
    // Nombre del corte (puede ser "Parcial", "Total" o "Sin ventas")
    public String nombre;

    // Info adicional que se muestra abajo del nombre (normalmente detalle del corte)
    public String info;

    // Status para identificar en qué estado está el corte (1, 2, 3, etc. — se usa para pintar colores del texto de los titulos)
    public int status;

    // Constructor para inicializar los datos de este objeto cuando se crea
    public CorteTotal(String nombre, String info, int status) {
        this.nombre = nombre;
        this.info = info;
        this.status = status;
    }
}
