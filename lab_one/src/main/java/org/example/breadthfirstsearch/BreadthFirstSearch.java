package org.example.breadthfirstsearch;

import org.example.DiagramGraphObjectsAccessor;
import org.example.dto.sld.Diagram;
import org.example.dto.sld.Element;
import org.example.dto.sld.Link;
import org.example.dto.sld.Port;

import java.util.*;


public class BreadthFirstSearch {
    private final Set<String> visitedNodeIds = new HashSet<>();

    private Diagram diagram;

    public BreadthFirstSearch(Diagram diagram) {
        this.diagram = diagram;
    }

    public void searchFromNode(
            Element element,
            ThreeArgumentsConsumer<Element, Link, Port> consumer
    ) {
        Queue<Element> queue = new LinkedList<>() {{
            add(element);
        }};
        walkThroughDiagramRecursively(queue, consumer);
    }

    private void walkThroughDiagramRecursively(
            Queue<Element> queue,
            ThreeArgumentsConsumer<Element, Link, Port> consumer
    ) {
        if (queue.isEmpty()) return;
        Element element = queue.poll();
        visitedNodeIds.add(element.getId());

        element.getPorts()
                .forEach(port -> {
                    if (!port.getLinks().isEmpty()) {
                        Link link = DiagramGraphObjectsAccessor.getLinkByPort(diagram, port);
                        Element sibling = DiagramGraphObjectsAccessor.getSiblingForElementByLink(diagram, element, link);
                        consumer.accept(element, link, port);
                        if (!visitedNodeIds.contains(sibling.getId())) {
                            queue.add(sibling);
                        }
                    }
                    walkThroughDiagramRecursively(queue, consumer);
                });
    }

}