package pe.com.sammis.vale.services;

import org.springframework.stereotype.Service;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.repositories.EmpleadoRepository;
import pe.com.sammis.vale.services.IEmpleadoService;

import java.util.List;
import java.util.Optional;

@Service
public class EmpleadoServiceImpl implements IEmpleadoService {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoServiceImpl(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    public List<Empleado> findAll() {
        return empleadoRepository.findAll();
    }

    @Override
    public Optional<Empleado> findById(Long id) {
        return empleadoRepository.findById(id);
    }

    @Override
    public Empleado save(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }

    @Override
    public Empleado update(Long id, Empleado empleado) {
        return empleadoRepository.findById(id).map(emp -> {
            emp.setNombre(empleado.getNombre());
            emp.setApellido(empleado.getApellido());
            return empleadoRepository.save(emp);
        }).orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con ID: " + id));
    }


    @Override
    public void deleteById(Long id) {
        empleadoRepository.deleteById(id);
    }

    @Override
    public Optional<Empleado> findByDni(String dni) {
        return empleadoRepository.findByDni(dni);
    }
}
