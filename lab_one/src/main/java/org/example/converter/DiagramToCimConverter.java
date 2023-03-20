package org.example.converter;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.example.dto.cim.IdentifiedObject;
import org.example.dto.cim.Substation;
import org.example.dto.sld.Diagram;
import org.example.writer.RdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiagramToCimConverter {

    private final String cimNamespace = "http://iec.ch/TC57/2013/COM-schema-cim16#";
    private final String rdfNameSpace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private ModelBuilder modelBuilder = new ModelBuilder();
    {
        modelBuilder
                .setNamespace("rdf", rdfNameSpace)
                .setNamespace("cim", cimNamespace);
    }

    public ModelBuilder convert(Diagram diagram,
                                String voltageLevelSource,
                                String devicesDefinitionSource) throws IOException {

        Substation substation = SldToCimConverter.convertDiagramToSubstation(
                diagram,
                voltageLevelSource,
                devicesDefinitionSource);
        return modelBuilder;
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
