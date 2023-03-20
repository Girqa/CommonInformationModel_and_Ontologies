package org.example;

import org.example.dto.sld.Diagram;
import org.example.dto.sld.Element;
import org.example.dto.sld.Link;
import org.example.dto.sld.Port;

public class DiagramGraphObjectsAccessor {

    public static Link getLinkByPort(Diagram diagram, Port port) {
        String linkId = port.getLinks().get(0);
        return diagram.getLinks().stream()
                .filter(link -> link.getId().equals(linkId))
                .findFirst().get();
    }

    public static Element getSiblingForElementByLink(
            Diagram diagram, Element element, Link link
    ) {
       String siblingId = link.getSourceId().equals(element.getId()) ? link.getTargetId() : link.getSourceId();
       return diagram.getElements().stream()
               .filter(e -> e.getId().equals(siblingId))
               .findFirst().get();
    }

}
