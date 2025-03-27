package pe.com.sammis.vale.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.models.TipoAsistencia;

public interface TipoAsistenciaRepository extends JpaRepository<TipoAsistencia,Long> {
}
