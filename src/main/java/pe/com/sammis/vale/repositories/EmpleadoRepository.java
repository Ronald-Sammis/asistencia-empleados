package pe.com.sammis.vale.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.sammis.vale.models.Empleado;

import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado,Long> {
    Optional<Empleado> findByDni(String dni);
}
