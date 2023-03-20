package org.example.dto.sld;

import lombok.Data;

import java.util.List;

@Data
public class Element {
    private String id;
    private ElementType type;
    private String directoryEntryId;
    private String voltageLevel;
    private String operationName;
    private String projectName;
    private List<Port> ports = List.of();
    private List<Field> fields  = List.of();
}
