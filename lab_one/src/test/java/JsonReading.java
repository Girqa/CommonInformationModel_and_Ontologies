import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.breadthfirstsearch.BreadthFirstSearch;
import org.example.dto.sld.Diagram;
import org.example.dto.sld.Element;
import org.example.mapper.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonReading {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Reading Viezdnoe.json and mapping to Java objects")
    public void read() throws IOException {
        JsonMapper jsonMapper = new JsonMapper();
        Diagram diagram = jsonMapper.mapJsonToDiagram("src/test/resources/Viezdnoe.json");

        BreadthFirstSearch breadthFirstSearch = new BreadthFirstSearch(
                diagram
        );
        breadthFirstSearch.searchFromNode(
                diagram.getElements().get(0),
                (element, link, port) -> {
                    if (element.getOperationName() == null) {
                        System.out.println(element.getType());
                    }
                }
        );
    }

    @Test
    @DisplayName("Check elements operation modes")
    public void checkOperationModesTest() throws IOException {
        Diagram diagram = objectMapper.readValue(new File("src/test/resources/Viezdnoe.json"), Diagram.class);
        Set<String> operationModes = diagram.getElements().stream()
                .map(Element::getOperationName)
                .collect(Collectors.toSet());
        System.out.println(operationModes.contains(null));
    }

    @Test
    @DisplayName("Check elements directory id modes")
    public void checkDirectoryIdTest() throws IOException {
        Diagram diagram = objectMapper.readValue(new File("src/test/resources/Viezdnoe.json"), Diagram.class);
        Set<String> directoryIds = diagram.getElements().stream()
                .map(Element::getDirectoryEntryId)
                .collect(Collectors.toSet());
        System.out.println(directoryIds.size());
    }

}
