package de.neebs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RequestFacade {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> get(String request) {
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(request, String.class);
            if (responseEntity.getBody() == null) {
                return null;
            }
            return objectMapper.readValue(responseEntity.getBody(), new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        } catch (HttpClientErrorException e) {
            return null;
        }
    }

    public void post(String request, Map<String, Object> body) {
        restTemplate.postForEntity(request, body, String.class);
    }

    public void put(String request, Map<String, Object> body) {
        restTemplate.put(request, body, String.class);
    }
}
