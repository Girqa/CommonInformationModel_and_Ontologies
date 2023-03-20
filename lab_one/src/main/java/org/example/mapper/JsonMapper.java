package org.example.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.example.dto.sld.Diagram;

import java.io.File;

public class JsonMapper {

    private ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public Diagram mapJsonToDiagram(String filePath) {
        return objectMapper.readValue(new File(filePath), Diagram.class);
    }
}
