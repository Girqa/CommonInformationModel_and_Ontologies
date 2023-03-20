package org.example.dto.cim;

import lombok.Data;
import lombok.NonNull;
import org.example.annotations.AssociationType;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfResource;


@Data
@RdfResource
public class Bay extends IdentifiedObject {
    @RdfAssociation(AssociationType.ONE_TO_ONE)
    protected ConnectivityNode connectivityNode;
    public Bay(@NonNull String mRID, String name) {
        super(mRID, "Bay", name);
    }
}
