package pe.com.sammis.vale.services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class APISunatServiceImpl {

    private static final String API_URL = "https://api.apis.net.pe/v2/";
    private static final String TOKEN = "Bearer apis-token-7967.jnTYhcOrD2QCmx87khoUgnFWgfBhJ7-J";

    public String consultarDocumento(String numeroDocumento) {
        // Fijo que siempre se consulta por "dni"
        String apiConsulta = API_URL + "reniec/dni?numero=" + numeroDocumento;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = new RestTemplate().exchange(apiConsulta, HttpMethod.GET, entity, String.class);

        return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    }

}
