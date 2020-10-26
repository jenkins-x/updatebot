/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.updatebot.maven;

import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.updatebot.maven.health.SpringBootHealthCheckEnricher;
import io.jenkins.updatebot.support.FileHelper;
import io.jenkins.updatebot.support.Strings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Fixes up a chart if we can detect any issues with the java dependencies.
 * <p>
 * e.g. fix up the liveness check URL based on the version of spring boot
 */
@Mojo(name = "chart", aggregator = true, requiresProject = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class FixChartMojo extends AbstractMojo {

    protected static final String PROBE_PREFIX = "probePath: ";
    protected static final String DEFAULT_PROBE_VALUE = "/actuator/health";

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${basedir}/charts/${project.artifactId}/values.yaml")
    protected File valuesFile;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        if (valuesFile == null) {
            log.warn("No values file not injected!");
            return;
        }
        if (!valuesFile.exists()) {
            log.warn("The values.yaml file: " + valuesFile + " does not exist");
            return;
        }
        if (project == null) {
            throw new MojoExecutionException("No Project available!");
        }

        SpringBootHealthCheckEnricher enricher = new SpringBootHealthCheckEnricher(project);
        Probe probe = enricher.discoverProbe(null, null);
        log.debug("Found spring boot probe: " + probe);
        if (probe != null) {
            HTTPGetAction httpGet = probe.getHttpGet();
            if (httpGet != null) {
                String path = httpGet.getPath();
                if (Strings.notEmpty(path)) {
                    log.debug("found spring boot probe path: " + path);
                    List<String> lines;
                    try {
                        lines = FileHelper.readLines(valuesFile);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to load " + valuesFile + ": " + e, e);
                    }
                    boolean modified = false;
                    int idx = 0;
                    for (String line : lines) {
                        if (line.startsWith(PROBE_PREFIX)) {
                            String value = Strings.trimPrefix(line, PROBE_PREFIX);
                            if (!value.equals(path)) {
                                lines.set(idx, PROBE_PREFIX + path);
                                modified = true;
                                break;
                            }
                        }
                        idx++;
                    }
                    if (modified) {
                        try {
                            FileHelper.writeLines(valuesFile, lines);
                            log.info("Updated helm chart values in: " + valuesFile + " setting probe path to: " + path);
                        } catch (IOException e) {
                            throw new MojoExecutionException("Failed to save " + valuesFile + ": " + e, e);
                        }
                    }
                }
            }
        }
    }
}
