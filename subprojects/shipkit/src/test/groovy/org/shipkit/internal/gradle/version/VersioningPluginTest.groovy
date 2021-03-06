package org.shipkit.internal.gradle.version

import org.shipkit.gradle.git.GitCommitTask
import org.shipkit.gradle.version.BumpVersionFileTask
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.git.GitPlugin
import org.shipkit.version.VersionInfo
import testutil.PluginSpecification

class VersioningPluginTest extends PluginSpecification {

    def "should initialize bumpVersionFile task properly"() {
        when:
        project.plugins.apply(VersioningPlugin)

        then:
        BumpVersionFileTask task = project.getTasks().findByName(VersioningPlugin.BUMP_VERSION_FILE_TASK)
        task.versionFile == project.file(VersioningPlugin.VERSION_FILE_NAME)
    }

    def "should set VersionInfo extension according to version.properties content if this file exists"() {
        given:
        project.file(VersioningPlugin.VERSION_FILE_NAME) << "version=1.0.0\nnotableVersions=0.1.0\n"
        when:
        project.plugins.apply(VersioningPlugin)

        then:
        def versionInfo = project.extensions.getByType(VersionInfo)
        versionInfo.versionFile == project.file(VersioningPlugin.VERSION_FILE_NAME)
        versionInfo.version == "1.0.0"
        versionInfo.notableVersions == ["0.1.0"] as LinkedList
    }

    def "should set VersionInfo extension to project.version defaults if version.properties doesn't exist"() {
        given:
        assert !project.file(VersioningPlugin.VERSION_FILE_NAME).exists()
        project.version = "1.2.3"

        when:
        project.plugins.apply(VersioningPlugin)

        then:
        def versionInfo = project.extensions.getByType(VersionInfo)
        versionInfo.versionFile == project.file(VersioningPlugin.VERSION_FILE_NAME)
        versionInfo.version == "1.2.3"
        versionInfo.notableVersions == [] as LinkedList
    }

    def "should add version bumped changes to GitCommitTask if GitPlugin applied"() {
        given:
        project.file(VersioningPlugin.VERSION_FILE_NAME) << "version=0.9.0\npreviousVersion=0.8.114\n"

        and:
        project.plugins.apply(ShipkitConfigurationPlugin).configuration.gitHub.repository = "http://github.com"
        project.plugins.apply(GitPlugin)

        when:
        project.plugins.apply(VersioningPlugin)

        then:
        GitCommitTask commit = project.tasks.getByName(GitPlugin.GIT_COMMIT_TASK)
        commit.filesToCommit.contains(project.file(VersioningPlugin.VERSION_FILE_NAME))
        commit.descriptions[0].contains("0.9.0 release (previous 0.8.114)")
    }

    def "should skip previous version in release commit message if not available"() {
        given:
        project.file(VersioningPlugin.VERSION_FILE_NAME) << "version=0.9.0\n"

        and:
        project.plugins.apply(ShipkitConfigurationPlugin).configuration.gitHub.repository = "http://github.com"
        project.plugins.apply(GitPlugin)

        when:
        project.plugins.apply(VersioningPlugin)

        then:
        GitCommitTask commit = project.tasks.getByName(GitPlugin.GIT_COMMIT_TASK)
        commit.filesToCommit.contains(project.file(VersioningPlugin.VERSION_FILE_NAME))
        commit.descriptions[0].contains("0.9.0 release")
        !commit.descriptions[0].contains("previous")
    }

    @Override
    void createShipkitConfiguration() {
        // ShipkitConfiguration is not needed in this test
    }
}
