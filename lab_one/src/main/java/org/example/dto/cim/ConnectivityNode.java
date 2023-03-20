package org.example.dto.cim;

import lombok.Data;
import lombok.NonNull;
import org.example.annotations.AssociationType;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfResource;

import java.util.List;

@Data
@RdfResource
public class ConnectivityNode extends IdentifiedObject {
    @RdfAssociation(AssociationType.MANY_TO_ONE)
    protected BaseVoltage baseVoltage;
    @RdfAssociation(AssociationType.ONE_TO_MANY)
    protected List<Terminal> terminals;

    public ConnectivityNode(@NonNull String mRID,
                            @NonNull String name,
                            BaseVoltage baseVoltage,
                            List<Terminal> terminals) {
        super(mRID, "ConnectivityNode", name);
        this.baseVoltage = baseVoltage;
        this.terminals = terminals;
    }

    public ConnectivityNode(@NonNull String mRID, @NonNull String name) {
        super(mRID, "ConnectivityNode", name);
    }
}
