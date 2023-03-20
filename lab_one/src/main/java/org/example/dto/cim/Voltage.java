package org.example.dto.cim;

import lombok.Data;

@Data
public class Voltage implements FloatDataType{
    private float value;
    public Voltage(float value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return "Voltage";
    }
}
