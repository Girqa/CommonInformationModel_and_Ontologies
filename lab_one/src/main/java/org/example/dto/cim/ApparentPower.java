package org.example.dto.cim;

import lombok.Data;

@Data
public class ApparentPower implements FloatDataType{
    private float power;

    public ApparentPower(float power) {
        this.power = power;
    }

    @Override
    public float getValue() {
        return power;
    }

    @Override
    public String getType() {
        return "ApparentPower";
    }
}
