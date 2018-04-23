/*
 * Copyright 2018 Original Authors
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
package io.jenkins.updatebot.kind.regex;

import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.jenkins.updatebot.commands.CommandContext;
import io.jenkins.updatebot.commands.PushRegexChanges;
import io.jenkins.updatebot.kind.UpdaterSupport;
import io.jenkins.updatebot.model.Dependencies;
import io.jenkins.updatebot.model.DependencyVersionChange;
import io.jenkins.updatebot.support.FileMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class RegexUpdater extends UpdaterSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(RegexUpdater.class);

    @Override
    public boolean isApplicable(CommandContext context) {
        return false;
    }

    @Override
    public void addVersionChangesFromSource(CommandContext context, Dependencies dependencyConfig, List<DependencyVersionChange> list) throws IOException {
    }

    @Override
    public boolean pushVersions(CommandContext parentContext, List<DependencyVersionChange> changes) throws IOException {
        return false;
    }

    public boolean pushRegex(PushRegexChanges command, CommandContext context) throws IOException {
        List<String> excludeFiles = command.getExcludeFiles();
        if (excludeFiles == null) {
            excludeFiles = Collections.EMPTY_LIST;
        }
        FileMatcher matcher = new FileMatcher(command.getFiles(), excludeFiles);
        List<File> files = matcher.matchFiles(context.getDir());
        boolean answer = false;
        for (File file : files) {
            if (doPushRegex(command, context, file)) {
                answer = true;
            }
        }
        return answer;
    }

    protected boolean doPushRegex(PushRegexChanges command, CommandContext context, File file) throws IOException {
        boolean answer = false;
        if (Files.isFile(file)) {
            String text = IOHelpers.readFully(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String[] lines = text.split("\n");
            String regex = command.getRegex();
            String value = command.getValue();

            Pattern pattern = Pattern.compile(regex);
            for (int i = 0, size = lines.length; i < size; i++) {
                String line = lines[i];
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    int start = m.start(1);
                    int end = m.end(1);
                    String newLine = line.substring(0, start) + value + line.substring(end);
                    if (!line.equals(newLine)) {
                        lines[i] = newLine;
                        answer = true;
                    }
                }
            }
            if (answer) {
                String updatedText = String.join(System.lineSeparator(), lines);
                Files.writeToFile(file, updatedText, StandardCharsets.UTF_8);
            }
        }
        return answer;
    }

}