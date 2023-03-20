package org.example.dto.cim;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class IdentifiedObject {
    @NonNull
    protected String mRID;
    @NonNull
    protected String type;
    @NonNull
    protected String name;
}
