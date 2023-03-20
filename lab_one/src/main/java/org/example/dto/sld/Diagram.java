package org.example.dto.sld;

import lombok.Data;

import java.util.List;

@Data
public class Diagram {
    private List<Element> elements;
    private List<Link> links;
}
