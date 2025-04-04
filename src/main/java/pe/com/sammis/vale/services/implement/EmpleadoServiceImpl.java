package pe.com.sammis.vale.services.implement;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.repositories.EmpleadoRepository;
import pe.com.sammis.vale.services.interfaces.IEmpleadoService;

import java.util.List;
import java.util.Optional;

@Service
public class EmpleadoServiceImpl implements IEmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private static final String API_URL = "https://api.apis.net.pe/v2/";
    private static final String TOKEN = "Bearer apis-token-7967.jnTYhcOrD2QCmx87khoUgnFWgfBhJ7-J";

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

    @Override
    public String consultaSunat(String dni) {
        // Fijo que siempre se consulta por "dni"
        String apiConsulta = API_URL + "reniec/dni?numero=" + dni;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = new RestTemplate().exchange(apiConsulta, HttpMethod.GET, entity, String.class);

        return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    }
}
