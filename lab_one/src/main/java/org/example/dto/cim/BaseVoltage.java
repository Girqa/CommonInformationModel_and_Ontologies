package org.example.dto.cim;

import lombok.Data;
import lombok.NonNull;
import org.example.annotations.RdfDataType;
import org.example.annotations.RdfResource;

@Data
@RdfResource
public class BaseVoltage extends IdentifiedObject {
    @RdfDataType
    protected Voltage nominalVoltage;

    public BaseVoltage(@NonNull String mRID, Voltage nominalVoltage) {
        super(mRID, "BaseVoltage", nominalVoltage + "kV");
        this.nominalVoltage = nominalVoltage;
    }
}
