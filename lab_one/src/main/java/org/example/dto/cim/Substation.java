package org.example.dto.cim;

import lombok.Data;
import lombok.NonNull;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfResource;

import java.util.List;

import static org.example.annotations.AssociationType.ONE_TO_MANY;

@Data
@RdfResource
public class Substation extends IdentifiedObject {
    @RdfAssociation(ONE_TO_MANY)
    protected List<Bay> bays;
    @RdfAssociation(ONE_TO_MANY)
    protected List<ConductingEquipment> equipments;
    @RdfAssociation(ONE_TO_MANY)
    protected List<ConnectivityNode> connectivityNodes;

    public Substation(@NonNull String mRID, @NonNull String name) {
        super(mRID, "Substation", name);
    }

    public Substation(@NonNull String mRID,
                      @NonNull String name,
                      List<Bay> bays,
                      List<ConductingEquipment> equipments,
                      List<ConnectivityNode> connectivityNodes) {
        super(mRID, "Substation", name);
        this.bays = bays;
        this.equipments = equipments;
        this.connectivityNodes = connectivityNodes;
    }
}
