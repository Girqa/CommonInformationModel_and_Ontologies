package org.example.dto.cim;

import lombok.Data;
import lombok.NonNull;
import org.example.annotations.AssociationType;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfResource;

import java.util.Objects;

@Data
@RdfResource
public class Terminal extends IdentifiedObject {
    @RdfAssociation(AssociationType.MANY_TO_ONE)
    protected ConductingEquipment conductingEquipment;
    @RdfAssociation(AssociationType.MANY_TO_ONE)
    protected ConnectivityNode connectivityNode;

    public Terminal(@NonNull String mRID, @NonNull String name) {
        super(mRID, "Terminal", name);
    }

    public Terminal(@NonNull String mRID,
                    @NonNull String name,
                    ConductingEquipment conductingEquipment,
                    ConnectivityNode connectivityNode) {
        super(mRID, "Terminal", name);
        this.conductingEquipment = conductingEquipment;
        this.connectivityNode = connectivityNode;
    }

    @Override
    public String toString() {
        return "Terminal{" +
                "mRID='" + mRID + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Terminal terminal)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(mRID, terminal.mRID) && Objects.equals(type, terminal.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mRID, type);
    }
}
