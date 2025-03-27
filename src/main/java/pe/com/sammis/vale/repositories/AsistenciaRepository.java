package pe.com.sammis.vale.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.sammis.vale.models.Asistencia;
import pe.com.sammis.vale.models.Empleado;

import java.time.LocalDate;
import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia,Long> {

    List<Asistencia> findByFechaBetween(LocalDate inicio, LocalDate fin);
    boolean existsByFecha(LocalDate fecha);
    @Query("SELECT DISTINCT a.fecha FROM Asistencia a ORDER BY a.fecha DESC")
    List<LocalDate> findDistinctFechas();


    List<Asistencia> findByFecha(LocalDate fecha);




}
