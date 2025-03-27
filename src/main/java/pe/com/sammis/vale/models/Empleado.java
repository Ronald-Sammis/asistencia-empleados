package pe.com.sammis.vale.models;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "tb_empleados")


public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellido;

    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL)
    private List<Asistencia> asistencias;

    public Empleado() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }


}
