package com.lrenyi.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.reporting.MavenMultiPageReport;
import org.apache.maven.reporting.MavenReportException;
import org.jacoco.maven.FileFilter;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.IReportVisitor;

@Mojo(name = "report-aggregate", threadSafe = true)
public class ReportAggregateMojo extends AbstractMojo implements MavenMultiPageReport {
    @Parameter(property = "jacoco.dataRootDir")
    private String dataRootDir;
    /**
     * Encoding of the source files.
     */
    @Parameter(property = "project.build.sourceEncoding", defaultValue = "UTF-8")
    String sourceEncoding;
    
    @Parameter(defaultValue = "HTML,XML,CSV")
    List<ReportFormat> formats;
    
    @Parameter(defaultValue = "${project.reporting.outputDirectory}/jacoco")
    private File outputDirectory;
    
    @Parameter(property = "project.reporting.outputEncoding", defaultValue = "UTF-8")
    String outputEncoding;
    
    @Parameter
    String footer;
    
    @Parameter(defaultValue = "${project.name}")
    String title;
    
    @Parameter(defaultValue = "false")
    private boolean includeCurrentProject;
    
    @Parameter(property = "project", readonly = true)
    MavenProject project;
    
    @Parameter(property = "session", readonly = true)
    MavenSession session;
    
    @Component
    private ProjectBuilder projectBuilder;
    
    /**
     * A list of class files to include in the report. May use wildcard
     * characters (* and ?). When not specified everything will be included.
     */
    @Parameter
    List<String> includes;
    
    /**
     * A list of class files to exclude from the report. May use wildcard
     * characters (* and ?). When not specified nothing will be excluded.
     */
    @Parameter
    List<String> excludes;
    
    @Parameter
    List<String> dataFileIncludes;
    @Parameter
    List<String> dataFileExcludes;
    
    @Override
    public void execute() throws MojoExecutionException {
        makeReport(Locale.getDefault());
    }
    
    private void makeReport(Locale locale) throws MojoExecutionException {
        try {
            final ReportSupport support = new ReportSupport(getLog());
            List<MavenProject> projectList = loadExecutionData(support);
            addFormatters(support, locale);
            final IReportVisitor visitor = support.initRootVisitor();
            createReport(visitor, support, projectList);
            visitor.visitEnd();
        } catch (final IOException e) {
            throw new MojoExecutionException("Error while creating report: " + e.getMessage(), e);
        }
    }
    
    void createReport(final IReportGroupVisitor visitor,
                      final ReportSupport support,
                      List<MavenProject> projectList) throws IOException {
        final IReportGroupVisitor group = visitor.visitGroup(title);
        if (includeCurrentProject) {
            processProject(support, group, project);
        }
        for (final MavenProject dependency : projectList) {
            processProject(support, group, dependency);
        }
    }
    
    private void processProject(final ReportSupport support,
                                final IReportGroupVisitor group,
                                final MavenProject project) throws IOException {
        support.processProject(group, project.getArtifactId(), project, getIncludes(), getExcludes(), sourceEncoding);
    }
    
    List<MavenProject> loadExecutionData(final ReportSupport support) throws IOException {
        if (dataFileIncludes == null) {
            dataFileIncludes = Collections.singletonList("target/*.exec");
        }
        final FileFilter filter = new FileFilter(dataFileIncludes, dataFileExcludes);
        final List<MavenProject> allProjects = new ArrayList<>();
        findAllProjects(dataRootDir, allProjects);
        for (final MavenProject dependency : allProjects) {
            loadExecutionData(support, filter, dependency.getBasedir());
        }
        return allProjects;
    }
    
    private void findAllProjects(String dataRootDir, List<MavenProject> allProjects) {
        if (dataRootDir == null || dataRootDir.isEmpty()) {
            findAllFileSetsByParentProject(project.getParent(), allProjects);
        } else {
            allProjects.add(project);
        }
    }
    
    private void loadExecutionData(final ReportSupport support,
                                   final FileFilter filter,
                                   final File basedir) throws IOException {
        for (final File execFile : filter.getFiles(basedir)) {
            support.loadExecutionData(execFile);
        }
    }
    
    private void addFormatters(final ReportSupport support, final Locale locale) throws IOException {
        if (!outputDirectory.exists()) {
            boolean mkdir = outputDirectory.mkdirs();
            if (!mkdir) {
                throw new IOException("Created directory fail: " + outputDirectory.getAbsolutePath());
            }
        }
        for (final ReportFormat f : formats) {
            support.addVisitor(f.createVisitor(this, locale));
        }
    }
    
    private void findAllFileSetsByParentProject(MavenProject parent, List<MavenProject> fileSets) {
        List<String> modules = parent.getModules();
        ProjectBuildingRequest buildingRequest = session.getProjectBuildingRequest();
        for (String module : modules) {
            File modulePom = new File(parent.getBasedir(), module + File.separator + "pom.xml");
            try {
                MavenProject moduleProject = projectBuilder.build(modulePom, buildingRequest).getProject();
                fileSets.add(moduleProject);
                List<String> list = moduleProject.getModules();
                if (list != null && !list.isEmpty()) {
                    findAllFileSetsByParentProject(moduleProject, fileSets);
                }
            } catch (Exception e) {
                getLog().info("parser fail: " + module);
            }
        }
    }
    
    @Override
    public void generate(Sink sink, SinkFactory sinkFactory, Locale locale) throws MavenReportException {
        if (!canGenerateReport()) {
            return;
        }
        try {
            makeReport(locale);
        } catch (MojoExecutionException e) {
            throw new MavenReportException("", e);
        }
    }
    
    @Override
    public void generate(org.codehaus.doxia.sink.Sink sink, Locale locale) throws MavenReportException {
        generate(sink, null, locale);
    }
    
    @Override
    public String getOutputName() {
        return "jacoco/index";
    }
    
    @Override
    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }
    
    @Override
    public String getName(Locale locale) {
        return "JaCoCo";
    }
    
    @Override
    public String getDescription(Locale locale) {
        return getName(locale) + " Coverage Report.";
    }
    
    @Override
    public void setReportOutputDirectory(File file) {
        if (file != null && !file.getAbsolutePath().endsWith("jacoco")) {
            outputDirectory = new File(file, "jacoco");
        } else {
            outputDirectory = file;
        }
    }
    
    @Override
    public File getReportOutputDirectory() {
        return outputDirectory;
    }
    
    @Override
    public boolean isExternalReport() {
        return true;
    }
    
    @Override
    public boolean canGenerateReport() {
        return true;
    }
    
    public List<String> getIncludes() {
        return includes;
    }
    
    public List<String> getExcludes() {
        return excludes;
    }
    
    public File getOutputDirectory() {
        return outputDirectory;
    }
}
