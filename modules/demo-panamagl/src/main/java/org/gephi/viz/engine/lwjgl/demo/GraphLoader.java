package org.gephi.viz.engine.lwjgl.demo;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerUnloader;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;

import java.io.File;
import java.io.FileNotFoundException;

/**
 *
 * @author Eduardo Ramos
 */
public class GraphLoader {

    public static GraphModel load(String path) {
        try {
            ProjectController projectController = Lookup.getDefault().lookup(ProjectController.class);
            GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
            ImportController importController = Lookup.getDefault().lookup(ImportController.class);

            projectController.newProject();
            File file = new File(path).getAbsoluteFile();
            
            Container container = importController.importFile(file);
            container.closeLoader();
            
            DefaultProcessor processor = new DefaultProcessor();
            
            processor.setWorkspace(projectController.getCurrentWorkspace());
            processor.setContainers(new ContainerUnloader[]{container.getUnloader()});
            processor.process();

            return graphController.getGraphModel();
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
