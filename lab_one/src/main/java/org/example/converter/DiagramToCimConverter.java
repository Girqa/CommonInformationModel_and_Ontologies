package org.example.converter;

import lombok.SneakyThrows;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.example.annotations.RdfAssociation;
import org.example.annotations.RdfDataType;
import org.example.annotations.RdfResource;
import org.example.dto.cim.FloatDataType;
import org.example.dto.cim.IdentifiedObject;
import org.example.dto.cim.Substation;
import org.example.dto.sld.Diagram;
import org.example.writer.RdfWriter;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DiagramToCimConverter {

    private final String cimNamespace = "http://iec.ch/TC57/2013/COM-schema-cim16#";
    private final String rdfNameSpace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private ModelBuilder modelBuilder = new ModelBuilder();
    {
        modelBuilder
                .setNamespace("rdf", rdfNameSpace)
                .setNamespace("cim", cimNamespace);
    }

    public Model convert(Diagram diagram,
                                String voltageLevelSource,
                                String devicesDefinitionSource) throws IOException {

        Substation substation = SldToCimConverter.convertDiagramToSubstation(
                diagram,
                voltageLevelSource,
                devicesDefinitionSource);

        Queue<IdentifiedObject> objectsToWrite = new ArrayDeque<>();
        objectsToWrite.addAll(substation.getBays());
        objectsToWrite.addAll(substation.getEquipments());
        objectsToWrite.addAll(substation.getConnectivityNodes());
        process(modelBuilder, objectsToWrite);

        return modelBuilder.build();
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
                        }
                        case ONE_TO_ONE, MANY_TO_ONE -> {
                            IdentifiedObject object = (IdentifiedObject) field.get(resource);
                            objectsToWrite.add(object);
                            builder.add("cim:" + resource.getType() + "." + object.getType(), "rdf:" + object.getMRID());
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

    public String getResult(RDFFormat rdfFormat, Model model) {

        if (rdfFormat.equals(RDFFormat.RDFXML)) {
            RdfWriter rdfWriter = new RdfWriter();
            return rdfWriter.writeXml(model);
        } else {
            OutputStream out = null;
            String cim;
            try {
                File tempFile = File.createTempFile("file", ".txt");
                out = new FileOutputStream(tempFile);
                Rio.write(model, out, cimNamespace, rdfFormat);
                cim = Files.readString(Path.of(tempFile.getPath()));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return cim;
        }
    }
}
