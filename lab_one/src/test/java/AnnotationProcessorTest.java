import lombok.SneakyThrows;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.*;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfDataType;
import org.example.annotations.RdfResource;
import org.example.converter.DiagramToCimConverter;
import org.example.converter.SldToCimConverter;
import org.example.dto.cim.*;
import org.example.dto.sld.Diagram;
import org.example.mapper.JsonMapper;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;

public class AnnotationProcessorTest {

    @SneakyThrows
    @Test
    void processAnnotationsTest() {
        ModelBuilder modelBuilder = new ModelBuilder()
                .setNamespace("cim", "http://iec.ch/TC57/2013/CIM-schema-cim16#");

        JsonMapper jsonMapper = new JsonMapper();
        Diagram diagram = jsonMapper.mapJsonToDiagram("src/test/resources/Viezdnoe.json");

        Substation substation = SldToCimConverter.convertDiagramToSubstation(
                diagram,
                "src/test/resources/VoltageLevelDirectory.json",
                "src/test/resources/DeviceDirectory.json");

        Queue<IdentifiedObject> objectsToWrite = new ArrayDeque<>();
        objectsToWrite.addAll(substation.getBays());
        objectsToWrite.addAll(substation.getEquipments());
        objectsToWrite.addAll(substation.getConnectivityNodes());
        process(modelBuilder, objectsToWrite);

        Model model = modelBuilder.build();
        String result = new DiagramToCimConverter().getResult(RDFFormat.RDFXML, model);

        PrintWriter writer = new PrintWriter(new FileOutputStream("src/test/resources/cim-rdf.xml"));
        try {
            writer.println(result);
        } finally {
            writer.close();
        }
    }

    @SneakyThrows
    private void process(ModelBuilder builder, Queue<IdentifiedObject> objectsToWrite) {
        Set<IdentifiedObject> uniqueObjectsSet = new HashSet<>();


        while (!objectsToWrite.isEmpty()) {
            IdentifiedObject object = objectsToWrite.poll();
            if (uniqueObjectsSet.add(object)) {
                goFromTopToDownRecursively(builder, object, objectsToWrite);
            }
        }
    }

    @SneakyThrows
    private void goFromTopToDownRecursively(
            ModelBuilder builder,
            IdentifiedObject resource,
            Queue<IdentifiedObject> objectsToWrite) {

        if (resource.getClass().isAnnotationPresent(RdfResource.class)) {
            // Объявили заголовк
            builder.subject("cim:" + resource.getMRID())
                    .add(RDF.TYPE, "cim:" + resource.getType())
                    .add("cim:" + resource.getType() + ".mRID", resource.getMRID())
                    .add("cim:" + resource.getType() + ".name", resource.getName());
            // Пошли проходить по связям
            Class<? extends IdentifiedObject> aClass = resource.getClass();
            Field[] declaredFields = aClass.getDeclaredFields();
            for (Field field: declaredFields) {
                // Если не забыли пометить анотацией и является наследником IdentifiedObject
                field.setAccessible(true);
                if (field.isAnnotationPresent(RdfAssociation.class) &&
                        field.get(resource) != null) {
                    RdfAssociation association = field.getAnnotation(RdfAssociation.class);
                    switch (association.value()) {
                        case ONE_TO_MANY -> {
                            List<IdentifiedObject> subs = (List<IdentifiedObject>) field.get(resource);
                            objectsToWrite.addAll(subs);
                            for (IdentifiedObject sub: subs) {
                                builder.add("cim:" + resource.getType() + "." + sub.getType(), "rdf:resource=" + sub.getMRID());
                            }
                        }
                        case ONE_TO_ONE, MANY_TO_ONE -> {
                            IdentifiedObject object = (IdentifiedObject) field.get(resource);
                            objectsToWrite.add(object);
                            builder.add("cim:" + resource.getType() + "." + object.getType(), "rdf:resource=" + object.getMRID());
                        }
                    }
                } else if (field.isAnnotationPresent(RdfDataType.class) &&
                        field.get(resource) != null) {
                    FloatDataType dataType = (FloatDataType) field.get(resource);
                    builder.add("cim:" + resource.getType() + "." + dataType.getType(), dataType.getValue());
                }
            }
        } else {
            System.err.println("Class mast be annotated with RdfResource annotation to be mapped to RDF");
        }
    }
}
