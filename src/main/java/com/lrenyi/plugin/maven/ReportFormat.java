package com.lrenyi.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;

/**
 * Configurable output formats for the report goals.
 */
public enum ReportFormat {
    
    /**
     * Multi-page html report.
     */
    HTML() {
        @Override
        public IReportVisitor createVisitor(final ReportAggregateMojo mojo, final Locale locale) throws IOException {
            final HTMLFormatter htmlFormatter = new HTMLFormatter();
            htmlFormatter.setOutputEncoding(mojo.outputEncoding);
            htmlFormatter.setLocale(locale);
            if (mojo.footer != null) {
                htmlFormatter.setFooterText(mojo.footer);
            }
            return htmlFormatter.createVisitor(new FileMultiReportOutput(mojo.getOutputDirectory()));
        }
    },
    
    /**
     * Single-file XML report.
     */
    XML() {
        @Override
        public IReportVisitor createVisitor(final ReportAggregateMojo mojo, final Locale locale) throws IOException {
            final XMLFormatter xml = new XMLFormatter();
            xml.setOutputEncoding(mojo.outputEncoding);
            return xml.createVisitor(Files.newOutputStream(new File(mojo.getOutputDirectory(), "jacoco.xml").toPath()));
        }
    },
    
    /**
     * Single-file CSV report.
     */
    CSV() {
        @Override
        public IReportVisitor createVisitor(final ReportAggregateMojo mojo, final Locale locale) throws IOException {
            final CSVFormatter csv = new CSVFormatter();
            csv.setOutputEncoding(mojo.outputEncoding);
            return csv.createVisitor(Files.newOutputStream(new File(mojo.getOutputDirectory(), "jacoco.csv").toPath()));
        }
    };
    
    public abstract IReportVisitor createVisitor(ReportAggregateMojo mojo, final Locale locale) throws IOException;
    
}
