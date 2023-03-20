package org.example.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.DiagramGraphObjectsAccessor;
import org.example.breadthfirstsearch.BreadthFirstSearch;
import org.example.dto.cim.*;
import org.example.dto.sld.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SldToCimConverter {

    private static ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    public static Map<Port, Terminal> getTerminalsByPorts(Diagram diagram) {
        Map<Port, Terminal> terminalByPort = new HashMap();
        diagram.getElements().forEach(e -> e.getPorts().forEach(p -> terminalByPort.put(p, new Terminal(p.getId(), p.getName()))));
        return terminalByPort;
    }

    public static List<Bay> getBays(Diagram diagram) {
        List<Bay> bays = new ArrayList<>();
        diagram.getElements().forEach(e -> {
            if (e.getType().equals(ElementType.bus)) {
                bays.add(new Bay(e.getId(), e.getProjectName()));
            }
        });
        return bays;
    }

    public static Map<String, BaseVoltage> getBaseVoltages(String source) throws IOException {
        VoltageLevel[] voltageLevels = objectMapper.readValue(
                new File(source),
                VoltageLevel[].class
        );

        Map<String, BaseVoltage> baseVoltageByDirectoryId = new HashMap<>();
        for (VoltageLevel voltageLevel: voltageLevels) {
            baseVoltageByDirectoryId.put(
                    voltageLevel.getDirectoryId(),
                    new BaseVoltage(voltageLevel.getValue().getEn(), new Voltage(voltageLevel.getFloatValue()))
            );
        }
        return baseVoltageByDirectoryId;
    }

    public static List<ConnectivityNode> getConnectivityNodes(Diagram diagram,
                                                              Map<Port, Terminal> terminalByPort,
                                                              Map<String, BaseVoltage> baseVoltages,
                                                              List<Bay> bays) {
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
        return connectivityNodes;
    }

    public static List<ConductingEquipment> getEquipment(Diagram diagram,
                                                         Map<Port, Terminal> terminalsByPorts) {

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
        return equipments;
    }

    public static Map<String, Device> getDevicesByIds(String source) throws IOException {
        Map<String, Device> deviceById = new HashMap<>();

        Device[] devices = objectMapper.readValue(new File(source), Device[].class);
        for (Device device: devices) {
            deviceById.put(device.getId(), device);
        }
        return deviceById;
    }

    public static void replaceConductiveEquipmentsToTransformers(Diagram diagram,
                                                                 Map<String, Device> devicesByIds,
                                                                 List<ConductingEquipment> equipment) {
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
                                "PTE_" + i.get() + "_" + curEquipment.getMRID(),
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
                        .filter(field -> field.getName().equals("ApparentPower"))
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
    }

    public static Substation convertDiagramToSubstation(Diagram diagram,
                                                          String voltageLevelSource,
                                                          String devicesDefinitionSource) throws IOException {
        Map<Port, Terminal> terminalsByPorts = getTerminalsByPorts(diagram);
        Map<String, BaseVoltage> baseVoltages = getBaseVoltages(voltageLevelSource);
        List<Bay> bays = getBays(diagram);
        List<ConnectivityNode> connectivityNodes = getConnectivityNodes(diagram, terminalsByPorts, baseVoltages, bays);
        List<ConductingEquipment> equipment = getEquipment(diagram, terminalsByPorts);
        replaceConductiveEquipmentsToTransformers(diagram, getDevicesByIds(devicesDefinitionSource), equipment);

        return new Substation(
                UUID.randomUUID().toString(),
                "Substation",
                bays,
                equipment,
                connectivityNodes
        );
    }
}
