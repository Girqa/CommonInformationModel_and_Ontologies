package org.example.dto.sld;

import lombok.Data;

import java.util.List;

@Data
public class Port {
    private String id;
    private String name;
    private List<String> links;
    private List<Field> fields;
}
