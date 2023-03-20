import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.example.DiagramGraphObjectsAccessor;
import org.example.breadthfirstsearch.BreadthFirstSearch;
import org.example.converter.SldToCimConverter;
import org.example.converter.DiagramToCimConverter;
import org.example.dto.cim.*;
import org.example.dto.sld.*;
import org.example.mapper.JsonMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CimConverterTest {
    private static Diagram diagram;

    @BeforeAll
    static void setup() {
        JsonMapper jsonMapper = new JsonMapper();
        diagram = jsonMapper.mapJsonToDiagram("src/test/resources/Viezdnoe.json");
    }

    @Test
    void extractTerminals() {
        Map<Port, Terminal> terminalByPort = new HashMap();
        diagram.getElements().forEach(e -> e.getPorts().forEach(p -> terminalByPort.put(p, new Terminal(p.getId(), p.getName()))));
        System.out.println(terminalByPort);
    }

    @Test
    void extractBaseVoltage() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        VoltageLevel[] voltageLevels = objectMapper.readValue(
                new File("src/main/resources/VoltageLevelDirectory.json"),
                VoltageLevel[].class
        );

        Map<String, BaseVoltage> baseVoltageByDirectoryId = new HashMap<>();
        for (VoltageLevel voltageLevel: voltageLevels) {
            baseVoltageByDirectoryId.put(
                    voltageLevel.getDirectoryId(),
                    new BaseVoltage(voltageLevel.getValue().getEn(), new Voltage(voltageLevel.getFloatValue()))
            );
        }
        System.out.println(baseVoltageByDirectoryId);
    }

    @SneakyThrows
    @Test
    void extractConnectivityNodes() {
        Map<Port, Terminal> terminalByPort = SldToCimConverter.getTerminalsByPorts(diagram);
        Map<String, BaseVoltage> baseVoltages = SldToCimConverter.getBaseVoltages("src/main/resources/VoltageLevelDirectory.json");
        List<Bay> bays = SldToCimConverter.getBays(diagram);
        AtomicInteger cnCounter = new AtomicInteger();

        // Выделили все link в connectivity node
        List<ConnectivityNode> connectivityNodes = new ArrayList<>();
        BreadthFirstSearch breadthFirstSearch = new BreadthFirstSearch(diagram);

        breadthFirstSearch.searchFromNode(diagram.getElements().get(0), (e, l, p) -> {
            Element sibling = DiagramGraphObjectsAccessor.getSiblingForElementByLink(diagram, e, l);
            Optional<Port> siblingPort = sibling.getPorts().stream()
                    .filter(pp -> pp.getLinks().contains(l.getId()))
                    .findFirst();

            siblingPort.ifPresent(port -> {
                ConnectivityNode connectivityNode = new ConnectivityNode(UUID.randomUUID().toString(), "CN" + cnCounter.getAndIncrement());
                Terminal t1 = terminalByPort.get(p);
                Terminal t2 = terminalByPort.get(port);

                if (t1.getConnectivityNode() == null && t2.getConnectivityNode() == null) {
                    t1.setConnectivityNode(connectivityNode);
                    t2.setConnectivityNode(connectivityNode);
                    connectivityNode.setBaseVoltage(baseVoltages.get(e.getVoltageLevel()));
                    ArrayList<Terminal> terminals = new ArrayList<>();
                    terminals.add(t1);
                    terminals.add(t2);
                    connectivityNode.setTerminals(terminals);
                    connectivityNodes.add(connectivityNode);
                }
            });
        });

        // Объединили все CN, соединенные с элементом connectivity в один CN
        diagram.getElements().forEach(e -> {
            if (e.getType().equals(ElementType.connectivity)) {
                ConnectivityNode newBigNode = new ConnectivityNode(UUID.randomUUID().toString(), "CN" + cnCounter.getAndIncrement());
                List<ConnectivityNode> connectedCNs = new ArrayList<>();
                e.getPorts().forEach(p -> {
                    if (!p.getLinks().isEmpty()) {
                        Terminal terminal = terminalByPort.get(p);
                        // Так как на текущей стадии, все CN включают лишь 2 терминала (свой и чужой) -> выкинули старый терминал
                        terminal.getConnectivityNode().getTerminals().remove(terminal);
                        connectedCNs.add(terminal.getConnectivityNode());
                        terminal.setConnectivityNode(newBigNode);
                        terminalByPort.remove(p);
                    }
                });
                // Осталось по одному терминалу
                List<Terminal> newBigNodeTerminals = new ArrayList<>();
                connectedCNs.forEach(cn -> {
                    if (!cn.getTerminals().isEmpty()) {
                        newBigNodeTerminals.add(cn.getTerminals().get(0));
                    }
                });

                newBigNode.setTerminals(newBigNodeTerminals);
                newBigNode.setBaseVoltage(connectedCNs.get(0).getBaseVoltage());
                connectivityNodes.removeAll(connectedCNs);
                connectivityNodes.add(newBigNode);
            }
        });

        // Объединили все CN, соединенные с элементом bus в один CN
        diagram.getElements().forEach(e -> {
            if (e.getType().equals(ElementType.bus)) {
                ConnectivityNode newBigNode = new ConnectivityNode(UUID.randomUUID().toString(), "CN" + cnCounter.getAndIncrement());
                List<ConnectivityNode> connectedCNs = new ArrayList<>();
                e.getPorts().forEach(p -> {
                    if (!p.getLinks().isEmpty()) {
                        Terminal terminal = terminalByPort.get(p);
                        // Так как на текущей стадии, все CN включают лишь 2 терминала (свой и чужой) -> выкинули старый терминал
                        terminal.getConnectivityNode().getTerminals().remove(terminal);
                        connectedCNs.add(terminal.getConnectivityNode());
                        terminal.setConnectivityNode(newBigNode);
                        terminalByPort.remove(p);
                    }
                });
                // Осталось 1..* терминалов
                List<Terminal> newBigNodeTerminals = new ArrayList<>();
                connectedCNs.forEach(cn -> {
                    if (!cn.getTerminals().isEmpty()) {
                        newBigNodeTerminals.addAll(cn.getTerminals());
                    }
                });

                newBigNode.setTerminals(newBigNodeTerminals);
                newBigNode.setBaseVoltage(connectedCNs.get(0).getBaseVoltage());
                connectivityNodes.removeAll(connectedCNs);
                connectivityNodes.add(newBigNode);
                Optional<Bay> bayOptional = bays.stream()
                        .filter(b -> b.getMRID().equals(e.getId()))
                        .findFirst();
                if (bayOptional.isPresent()) {
                    bayOptional.get().setConnectivityNode(newBigNode);
                }
            }
        });
        // Проверка, что все CN с уникальными именами и mRID
        Set<String> names = new HashSet<>();
        Assertions.assertTrue(
                connectivityNodes.stream()
                        .map(ConnectivityNode::getName)
                        .filter(name -> !names.add(name))
                        .count() == 0);
        Set<String> mRIDs = new HashSet<>();
        Assertions.assertTrue(
                connectivityNodes.stream()
                        .map(ConnectivityNode::getMRID)
                        .filter(mRID -> !mRIDs.add(mRID))
                        .count() == 0);
        System.out.println();
    }

    @SneakyThrows
    @Test
    void connectEquipmentToCNTest() {
        Map<Port, Terminal> terminalsByPorts = SldToCimConverter.getTerminalsByPorts(diagram);
        List<ConductingEquipment> equipments = new ArrayList<>();

        diagram.getElements().forEach(e -> {
            if (e.getType().equals(ElementType.directory)) {
                List<Terminal> terminals = new ArrayList<>();
                e.getPorts().forEach(p -> {
                    terminals.add(terminalsByPorts.get(p));
                });
                ConductingEquipment equipment = new ConductingEquipment(
                        e.getId(),
                        e.getProjectName(),
                        terminals.get(0).getConnectivityNode().getBaseVoltage(),
                        terminals
                );
                equipments.add(equipment);
                terminals.forEach(t -> t.setConductingEquipment(equipment));
            }
        });
        Assertions.assertTrue(equipments.stream()
                .filter(e -> e.getTerminals().isEmpty())
                .count() == 0);
        System.out.println();
    }

    @SneakyThrows
    @Test
    void extractTransformers() {
        Map<Port, Terminal> terminalsByPorts = SldToCimConverter.getTerminalsByPorts(diagram);
        // Проинициализировал переменные
        SldToCimConverter.getConnectivityNodes(
                diagram,
                terminalsByPorts,
                SldToCimConverter.getBaseVoltages("src/test/resources/VoltageLevelDirectory.json"),
                SldToCimConverter.getBays(diagram));
        List<ConductingEquipment> equipment = SldToCimConverter.getEquipment(diagram, terminalsByPorts);
        Map<String, Device> devicesByIds = SldToCimConverter.getDevicesByIds("src/test/resources/DeviceDirectory.json");

        // Выделили все трансформаторы из Diagram
        Map<String, Element> transformerElementById = new HashMap<>();
        diagram.getElements().forEach(e -> {
            if (e.getType().equals(ElementType.directory) &&
                    devicesByIds.get(e.getDirectoryEntryId()).getDeviceType().contains("ThreeWindingPowerTransformer")) {
                transformerElementById.put(e.getId(), e);
            }
        });

        Iterator<ConductingEquipment> equipmentIterator = equipment.iterator();
        List<PowerTransformer> transformers = new ArrayList<>();
        // Заменили все ConductiveEquipment, соответствующие ТР-ам
        while (equipmentIterator.hasNext()) {
            ConductingEquipment curEquipment = equipmentIterator.next();

            if (transformerElementById.containsKey(curEquipment.getMRID())) {

                List<Terminal> terminals = curEquipment.getTerminals();
                List<PowerTransformerEnd> powerTransformerEnds = new ArrayList<>();
                AtomicInteger i = new AtomicInteger(1);
                PowerTransformer transformer = new PowerTransformer(curEquipment.getMRID(), curEquipment.getName());

                terminals.forEach(t -> powerTransformerEnds.add(
                        new PowerTransformerEnd(
                                "PTE_" + curEquipment.getMRID(),
                                curEquipment.getName() + "_PTE" + i.getAndIncrement(),
                                transformer,
                                curEquipment.getBaseVoltage(),
                                t
                        )
                ));
                transformer.setBaseVoltage(curEquipment.getBaseVoltage());
                transformer.setTerminals(curEquipment.getTerminals());
                transformer.setPowerTransformerEnds(powerTransformerEnds);
                Optional<Field> apparentPower = transformerElementById.get(curEquipment.getMRID())
                        .getFields().stream()
                        .filter(field -> field.equals("ApparentPower"))
                        .findFirst();
                if (apparentPower.isPresent()) {
                    transformer.setPower(new ApparentPower(Float.parseFloat(apparentPower.get().getValue())));
                } else {
                    transformer.setPower(new ApparentPower(-1));
                }
                transformer.getTerminals().forEach(t -> t.setConductingEquipment(transformer));
                transformers.add(transformer);
                equipmentIterator.remove();
            }
        }
        equipment.addAll(transformers);
        System.out.println();
    }

    @SneakyThrows
    @Test
    void getSubstationTest() {
        Substation substation = SldToCimConverter.convertDiagramToSubstation(
                diagram,
                "src/test/resources/VoltageLevelDirectory.json",
                "src/test/resources/DeviceDirectory.json");
        System.out.println();
    }
}
