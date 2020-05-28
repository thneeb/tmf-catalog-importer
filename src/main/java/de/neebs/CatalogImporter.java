package de.neebs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CatalogImporter {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QueryViaSql queryViaSql;

    @Autowired
    private RequestFacade requestFacade;

    @Value("${target}")
    private String target;

    public void run(String... args) throws Exception {
        String path = "src/test/resources";
        File directory = new File(path);
        String[] orderedList = directory.list();
        if (orderedList == null) {
            throw new IllegalStateException();
        }
        Arrays.sort(orderedList);
        for (String s : orderedList) {
            File file = new File(path + File.separator + s);
            if (file.isDirectory()) {
                processDirectory(file);
            } else {
                if (file.getCanonicalPath().endsWith("json")) {
                    processFile(file);
                }
            }
        }
    }

    private void processDirectory(File file) throws IOException {
        for (File f : Objects.requireNonNull(file.listFiles())) {
            if (f.getCanonicalPath().endsWith("json")) {
                processFile(f);
            }
        }
    }

    private void processFile(File file) {
        try {
            Mapping mapping = objectMapper.readValue(file, Mapping.class);
            List<Map<String, Object>> list = queryViaSql.query(mapping.getQuery());
            for (Map<String, Object> map : list) {
                map.put("target", target);
                VelocityContext context = new VelocityContext(map);
                StringWriter writer = new StringWriter();
                Velocity.evaluate(context, writer, "#", mapping.getRequest());
                String request = target + writer.toString();
                Map<String, Object> response = requestFacade.get(request);
                System.out.println(response);
                final boolean create;
                if (response == null) {
                    create = true;
                    response = new HashMap<>();
                } else {
                    create = false;
                }
                final String str;
                final String reference = (String)mapping.getMapping().get("$ref");
                if (reference != null) {
                    StringBuilder contentBuilder = new StringBuilder();
                    try (Stream<String> stream = Files.lines(Paths.get(file.getParent() + File.separator + reference), StandardCharsets.UTF_8)) {
                        stream.forEach(s -> contentBuilder.append(s).append(System.lineSeparator()));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                    str = contentBuilder.toString();
                } else {
                    str = objectMapper.writeValueAsString(mapping.getMapping());
                }
                writer = new StringWriter();
                Velocity.evaluate(context, writer, "#", str);
                String s = writer.toString();
                Map<String, Object> merge = objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
                response = mergeObject(response, merge, mapping.getBehaviour());
                System.out.println(response);
                if (create) {
                    requestFacade.post(request.substring(0, request.lastIndexOf('/')), response);
                } else {
                    requestFacade.put(request, response);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> mergeObject(Map<String, Object> response, Map<String, Object> merge, Map<String, Object> behaviour) {
        for (Map.Entry<String, Object> entry : merge.entrySet()) {
            if (entry.getValue().getClass().isPrimitive() || entry.getValue() instanceof String || entry.getValue() instanceof Date || entry.getValue() instanceof Number || entry.getValue() instanceof Boolean) {
                response.put(entry.getKey(), entry.getValue());
            } else if (entry.getValue() instanceof Map) {
                Map<String, Object> map = (Map<String, Object>)response.get(entry.getKey());
                if (map == null) {
                    map = new HashMap<>();
                }
                if (behaviour != null) {
                    response.put(entry.getKey(), mergeObject(map, (Map<String, Object>)entry.getValue(), (Map<String, Object>)behaviour.get(entry.getKey())));
                } else {
                    response.put(entry.getKey(), mergeObject(map, (Map<String, Object>)entry.getValue(), null));
                }
            } else if (entry.getValue() instanceof List) {
                String key;
                if (behaviour != null) {
                    Map<String, Object> map = (Map<String, Object>)behaviour.get(entry.getKey());
                    if (map != null) {
                        key = (String) map.get("key");
                    } else {
                        key = null;
                    }
                } else {
                    key = null;
                }
                if (key == null) {
                    response.put(entry.getKey(), entry.getValue());
                } else {
                    List<Map<String, Object>> mergeList = (List<Map<String, Object>>)entry.getValue();
                    List<Map<String, Object>> baseList = (List<Map<String, Object>>)response.get(entry.getKey());
                    if (baseList == null) {
                        baseList = new ArrayList<>();
                        response.put(entry.getKey(), baseList);
                    }
                    for (Map<String, Object> listEntry : mergeList) {
                        Object keyValue = listEntry.get(key);
                        Optional<Map<String, Object>> optional = baseList.stream().filter(f -> f.get(key).equals(keyValue)).findAny();
                        if (optional.isPresent()) {
                            mergeObject(optional.get(), listEntry, (Map<String, Object>)behaviour.get(entry.getKey()));
                        } else {
                            baseList.add(mergeObject(listEntry, listEntry, (Map<String, Object>)behaviour.get(entry.getKey())));
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Unknown class type: "+ entry.getValue().getClass() + " - " + entry.getValue());
            }
        }
        return response;
    }
}
