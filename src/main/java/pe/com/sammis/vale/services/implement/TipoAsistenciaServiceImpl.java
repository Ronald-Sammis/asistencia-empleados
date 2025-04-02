package pe.com.sammis.vale.services.implement;

import org.springframework.stereotype.Service;
import pe.com.sammis.vale.models.TipoAsistencia;
import pe.com.sammis.vale.repositories.TipoAsistenciaRepository;
import pe.com.sammis.vale.services.Interfaces.ITipoAsistenciaService;

import java.util.List;
import java.util.Optional;

@Service
public class TipoAsistenciaServiceImpl implements ITipoAsistenciaService {


    private final TipoAsistenciaRepository tipoAsistenciaRepository;

    public TipoAsistenciaServiceImpl(TipoAsistenciaRepository tipoAsistenciaRepository) {
        this.tipoAsistenciaRepository = tipoAsistenciaRepository;
    }

    @Override
    public List<TipoAsistencia> findAll() {
        return tipoAsistenciaRepository.findAll();
    }

    @Override
    public Optional<TipoAsistencia> findById(Long id) {
        return tipoAsistenciaRepository.findById(id);
    }

    @Override
    public TipoAsistencia save(TipoAsistencia tipoAsistencia) {
        return tipoAsistenciaRepository.save(tipoAsistencia);
    }

    @Override
    public TipoAsistencia update(Long id, TipoAsistencia tipoAsistencia) {
        return tipoAsistenciaRepository.findById(id).map(emp -> {
            emp.setNombre(tipoAsistencia.getNombre());
            emp.setColorHex(tipoAsistencia.getColorHex());
            return tipoAsistenciaRepository.save(emp);
        }).orElseThrow(() -> new IllegalArgumentException("Tipo de asistencia no encontrado con ID: " + id));
    }

    @Override
    public void deleteById(Long id) {
        tipoAsistenciaRepository.deleteById(id);
    }
}
