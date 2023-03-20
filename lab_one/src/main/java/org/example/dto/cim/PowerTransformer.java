package org.example.dto.cim;

import lombok.Data;
import lombok.NonNull;
import org.example.annotations.AssociationType;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfDataType;
import org.example.annotations.RdfResource;

import java.util.List;

@Data
@RdfResource
public class PowerTransformer extends ConductingEquipment {
    @RdfAssociation(AssociationType.ONE_TO_MANY)
    protected List<PowerTransformerEnd> powerTransformerEnds;
    @RdfDataType
    protected ApparentPower power;

    @RdfAssociation(AssociationType.MANY_TO_ONE)
    protected BaseVoltage baseVoltage;

    @RdfAssociation(AssociationType.ONE_TO_MANY)
    protected List<Terminal> terminals;


    public PowerTransformer(@NonNull String mRID, @NonNull String name) {
        super(mRID, name);
        this.type = "PowerTransformer";
    }

    public PowerTransformer(@NonNull String mRID,
                            @NonNull String name,
                            BaseVoltage baseVoltage,
                            List<Terminal> terminals,
                            List<PowerTransformerEnd> powerTransformerEnds,
                            ApparentPower power) {
        super(mRID, name, baseVoltage, terminals);
        this.type = "PowerTransformer";
        this.powerTransformerEnds = powerTransformerEnds;
        this.power = power;
    }
}
