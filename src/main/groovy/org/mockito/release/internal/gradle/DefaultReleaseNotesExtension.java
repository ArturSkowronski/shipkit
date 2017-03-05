package org.mockito.release.internal.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.mockito.release.gradle.notes.ReleaseNotesExtension;
import org.mockito.release.notes.Notes;
import org.mockito.release.notes.NotesBuilder;
import org.mockito.release.notes.format.MultiReleaseNotesFormatter;
import org.mockito.release.notes.format.ReleaseNotesFormatters;
import org.mockito.release.notes.generator.ReleaseNotesGenerator;
import org.mockito.release.notes.generator.ReleaseNotesGenerators;
import org.mockito.release.notes.model.ReleaseNotesData;

import java.io.File;
import java.util.*;

public class DefaultReleaseNotesExtension implements ReleaseNotesExtension {

    private static final Logger LOG = Logging.getLogger(DefaultReleaseNotesExtension.class);

    private File notesFile;
    private String authToken = "a0a4c0f41c200f7c653323014d6a72a127764e17";

    private final String version;
    private final File workDir;
    private final String extensionName;
    private final Map<String, String> labels = new LinkedHashMap<String, String>();

    public DefaultReleaseNotesExtension(File workDir, String version, String extensionName) {
        this.workDir = workDir;
        this.version = version;
        this.extensionName = extensionName;
    }

    private void assertConfigured() {
        if (notesFile == null || !notesFile.isFile()) {
            throw new GradleException("'notesFile' must be configured and the file must be present.\n"
                    + "Example: " + extensionName + ".notesFile = project.file(\'docs/release-notes.md\')");
        }

        if (authToken == null || authToken.trim().isEmpty()) {
            throw new GradleException("'authToken' must be configured.\n"
                    + "Example: " + extensionName + ".authToken = \'secret\'");
        }

    }

    @Override
    public String getPreviousVersion() {
        assertConfigured();
        String firstLine = FileUtil.firstLine(notesFile);
        return Notes.previousVersion(firstLine).getPreviousVersion();
    }

    @Override
    public String getReleaseNotes() {
        assertConfigured();
        LOG.lifecycle("Building new release notes based on {}", notesFile);
        NotesBuilder builder = Notes.gitHubNotesBuilder(workDir, authToken);
        String prev = "v" + getPreviousVersion();
        String current = "HEAD";
        LOG.lifecycle("Building notes for revisions: {} -> {}", prev, current);
        String newContent = builder.buildNotes(version, prev, current, labels);
        return newContent;
    }

    @Override
    public void updateReleaseNotes() {
        String newContent = getReleaseNotes();
        FileUtil.appendToTop(newContent, notesFile);
        LOG.lifecycle("Successfully updated release notes!");
    }

    public String getCompleteReleaseNotes() {
        //in progress
        ReleaseNotesGenerator generator = ReleaseNotesGenerators.releaseNotesGenerator(workDir, authToken);
        Collection<ReleaseNotesData> releaseNotes = generator.generateReleaseNotesData(new ArrayList<String>(Arrays.asList("2.7.5", "2.7.4", "2.7.3")), "v", new ArrayList(), true);
        MultiReleaseNotesFormatter formatter = ReleaseNotesFormatters.detailedFormatter("Detailed release notes:\n\n", labels, "https://github.com/mockito/mockito/compare/{0}...{1}");
        return formatter.formatReleaseNotes(releaseNotes);
    }

    @Override
    public File getNotesFile() {
        return notesFile;
    }

    @Override
    public void setNotesFile(File notesFile) {
        this.notesFile = notesFile;
    }

    @Override
    public String getAuthToken() {
        return authToken;
    }

    @Override
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }
}
