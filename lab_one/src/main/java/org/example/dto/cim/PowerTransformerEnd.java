package org.example.dto.cim;

import lombok.Data;
import lombok.NonNull;
import org.example.annotations.AssociationType;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfResource;

import java.util.Objects;

@Data
@RdfResource
public class PowerTransformerEnd extends IdentifiedObject {
    @RdfAssociation(AssociationType.MANY_TO_ONE)
    private PowerTransformer powerTransformer;
    @RdfAssociation(AssociationType.MANY_TO_ONE)
    private BaseVoltage baseVoltage;
    @RdfAssociation(AssociationType.ONE_TO_ONE)
    private Terminal terminal;

    public PowerTransformerEnd(@NonNull String mRID,
                               @NonNull String name,
                               PowerTransformer powerTransformer,
                               BaseVoltage baseVoltage,
                               Terminal terminal) {
        super(mRID, "PowerTransformerEnd", name);
        this.powerTransformer = powerTransformer;
        this.baseVoltage = baseVoltage;
        this.terminal = terminal;
    }

    @Override
    public String toString() {
        return "PowerTransformerEnd{" +
                "baseVoltage=" + baseVoltage +
                ", terminal=" + terminal +
                ", mRID='" + mRID + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PowerTransformerEnd that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(mRID, that.mRID) && Objects.equals(type, that.type) && Objects.equals(name, that.name) && Objects.equals(baseVoltage, that.baseVoltage) && Objects.equals(terminal, that.terminal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mRID, type, name, baseVoltage, terminal);
    }
}
