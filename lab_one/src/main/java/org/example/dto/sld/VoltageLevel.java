package org.example.dto.sld;

import lombok.Data;

@Data
public class VoltageLevel {
    private String directoryId;
    private Value value;
    private float floatValue;

    @Data
    public class Value {
        private String en;
        private String ru;
    }
}
