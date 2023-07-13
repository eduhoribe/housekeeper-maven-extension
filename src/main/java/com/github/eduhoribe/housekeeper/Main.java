package com.github.eduhoribe.housekeeper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.*;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.inject.Named;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Named("housekeeper-maven-extension")
public class Main extends AbstractMavenLifecycleParticipant {

    private static final Log LOG = new SystemStreamLog();
    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private static final String PLUGINS_XML = ".mvn/plugins.xml";
    private static final String DEPENDENCY_MANAGEMENT_XML = ".mvn/dependency_management.xml";

    private static List<Dependency> getDependencies(String rootDirectory) throws MavenExecutionException {
        Xpp3Dom configuration;
        Path config = Paths.get(rootDirectory, DEPENDENCY_MANAGEMENT_XML);
        if (Files.isRegularFile(config)) {
            if (LOG.isDebugEnabled()) LOG.debug("Dependency management file found");

            try (Reader reader = Files.newBufferedReader(config, Charset.defaultCharset())) {
                configuration = Xpp3DomBuilder.build(reader);
                if (LOG.isDebugEnabled()) LOG.debug("Dependency management file was read");

            } catch (XmlPullParserException | IOException e) {
                throw new MavenExecutionException("Failed to read " + DEPENDENCY_MANAGEMENT_XML, e);
            }
        } else {
            LOG.warn("Dependency management file not found in " + DEPENDENCY_MANAGEMENT_XML);
            return Collections.emptyList();
        }

        List<Dependency> dependencies;
        try {
            dependencies = Arrays.stream(configuration.getChildren("dependency")).map(Main::buildDependency).collect(Collectors.toList());

            if (dependencies.isEmpty() && LOG.isDebugEnabled()) {
                LOG.debug("No dependency found");
            }

        } catch (Exception e) {
            throw new MavenExecutionException("Failed to get dependencies", e);
        }
        return dependencies;
    }

    private static List<Plugin> getPlugins(String rootDirectory) throws MavenExecutionException {
        Xpp3Dom configuration;
        Path config = Paths.get(rootDirectory, PLUGINS_XML);
        if (Files.isRegularFile(config)) {
            if (LOG.isDebugEnabled()) LOG.debug("Plugin file found");

            try (Reader reader = Files.newBufferedReader(config, Charset.defaultCharset())) {
                configuration = Xpp3DomBuilder.build(reader);
                if (LOG.isDebugEnabled()) LOG.debug("Plugin file was read");

            } catch (XmlPullParserException | IOException e) {
                throw new MavenExecutionException("Failed to read " + PLUGINS_XML, e);
            }
        } else {
            LOG.warn("Plugin file not found in " + PLUGINS_XML);
            return Collections.emptyList();
        }

        List<Plugin> plugins;
        try {
            plugins = Arrays.stream(configuration.getChildren("plugin")).map(Main::buildPlugin).collect(Collectors.toList());
            if (plugins.isEmpty() && LOG.isDebugEnabled()) {
                LOG.debug("No plugin found");
            }

        } catch (Exception e) {
            throw new MavenExecutionException("Failed to get plugins", e);
        }

        return plugins;
    }

    private static Dependency buildDependency(Xpp3Dom dependencyDom) {
        try {
            return XML_MAPPER.readValue(dependencyDom.toString(), Dependency.class);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error to read dependency dom", e);
        }
    }

    private static Plugin buildPlugin(Xpp3Dom pluginDom) {
        try {
            Plugin plugin = XML_MAPPER.readValue(pluginDom.toString(), Plugin.class);

            /*
            After parsing the XML into a 'Plugin' object, the 'configuration' field is filled with a 'LinkedHashMap' object
            since it's declared as an 'Object' inside the 'Plugin' class, but later on the build a 'ClassCastException' is thrown
            because the object is not an instance of 'Xpp3Dom'.

            The code below resolves this problem by finding the configuration as a 'Xpp3Dom' object using the original XML object.
            */
            for (PluginExecution execution : plugin.getExecutions()) {

                Xpp3Dom[] executionsDom = pluginDom.getChild("executions").getChildren("execution");
                for (Xpp3Dom executionDom : executionsDom) {
                    if (execution.getId().equals(executionDom.getChild("id").getValue())) {
                        execution.setConfiguration(executionDom.getChild("configuration"));
                    }
                }
            }
            return plugin;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error to read plugin dom", e);
        }
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        String rootDirectory = session.getExecutionRootDirectory();

        List<Dependency> dependencies = getDependencies(rootDirectory);
        List<Plugin> plugins = getPlugins(rootDirectory);

        for (MavenProject project : session.getProjects()) {
            // Inject plugins
            Build build = Optional.ofNullable(project.getBuild()).orElseGet(Build::new);
            project.setBuild(build);

            plugins.forEach(build::addPlugin);

            // Inject dependencies
            Model model = Optional.ofNullable(project.getModel()).orElseGet(Model::new);
            DependencyManagement dependencyManagement = Optional.ofNullable(model.getDependencyManagement()).orElseGet(DependencyManagement::new);

            model.setDependencyManagement(dependencyManagement);
            project.setModel(model);

            dependencies.forEach(dependencyManagement::addDependency);
        }
    }
}
