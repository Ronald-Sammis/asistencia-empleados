package pe.com.sammis.vale.services.Interfaces;

import pe.com.sammis.vale.models.TipoAsistencia;

import java.util.List;
import java.util.Optional;

public interface ITipoAsistenciaService {
    List<TipoAsistencia> findAll();
    Optional<TipoAsistencia> findById(Long id);
    TipoAsistencia save(TipoAsistencia tipoAsistencia);
    TipoAsistencia update(Long id, TipoAsistencia tipoAsistencia);
    void deleteById(Long id);

}
