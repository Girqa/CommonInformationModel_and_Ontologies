package org.example.dto.cim;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.example.annotations.AssociationType;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfResource;

import java.util.List;

@Data
@RdfResource
@NoArgsConstructor
public class ConductingEquipment extends IdentifiedObject {
    @RdfAssociation(AssociationType.MANY_TO_ONE)
    protected BaseVoltage baseVoltage;
    @RdfAssociation(AssociationType.ONE_TO_MANY)
    protected List<Terminal> terminals;

    public ConductingEquipment(@NonNull String mRID, @NonNull String name) {
        super(mRID, "ConductingEquipment", name);
    }

    public ConductingEquipment(@NonNull String mRID,
                               @NonNull String name,
                               BaseVoltage baseVoltage,
                               List<Terminal> terminals) {
        super(mRID, "ConductingEquipment", name);
        this.baseVoltage = baseVoltage;
        this.terminals = terminals;
    }
}
