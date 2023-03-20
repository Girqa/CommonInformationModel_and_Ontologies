package org.example.dto.sld;

import lombok.Data;

@Data
public class Link {
    private String id;
    private String sourceId;
    private String sourcePortId;
    private String targetId;
    private String targetPortId;
}
