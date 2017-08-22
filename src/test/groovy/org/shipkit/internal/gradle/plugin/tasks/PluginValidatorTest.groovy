package org.shipkit.internal.gradle.plugin.tasks

import testutil.PluginSpecification

class PluginValidatorTest extends PluginSpecification {

    private static final String META_INF_GRADLE_PLUGINS = 'src/main/resources/META-INF/gradle-plugins'
    private static final String PLUGIN_PACKAGE = 'src/main/groovy/org/shipkit/gradle/'

    def "validate plugin properties files"(propertiesFileName, className, extension) {
        given:
        project.file(META_INF_GRADLE_PLUGINS).mkdirs()
        project.file(PLUGIN_PACKAGE).mkdirs()
        Set propertiesFiles = [project.file("$META_INF_GRADLE_PLUGINS/${propertiesFileName}.properties") << "implementation-class=org.shipkit.gradle.$className"]
        Set pluginFiles = [project.file("$PLUGIN_PACKAGE/${className}.${extension}") << "some content"]

        when:
        new PluginValidator().validate(pluginFiles, propertiesFiles)
        then:
        noExceptionThrown()

        where:
        propertiesFileName      | className          | extension
        'org.shipkit.test'      | 'TestPlugin'       | 'java'
        'org.shipkit.test'      | 'TestPlugin'       | 'groovy'
        'org.shipkit.my-sample' | 'MySamplePlugin'   | 'java'
        'org.shipkit.my-sample' | 'MySamplePlugin'   | 'groovy'
    }

    def "validate wrong properties file content"(propertiesContent, errorMessage) {
        given:
        project.file(META_INF_GRADLE_PLUGINS).mkdirs()
        project.file(PLUGIN_PACKAGE).mkdirs()
        Set propertiesFiles = [project.file("$META_INF_GRADLE_PLUGINS/org.shipkit.my-sample.properties") << propertiesContent]
        Set pluginFiles = [project.file("$PLUGIN_PACKAGE/MySamplePlugin.groovy") << "some content"]

        when:
        new PluginValidator().validate(pluginFiles, propertiesFiles)
        then:
        RuntimeException ex = thrown()
        ex.message.contains errorMessage

        where:
        propertiesContent                                       | errorMessage
        "implementation-class=org.shipkit.gradle.AnotherClass"  | 'src/main/resources/META-INF/gradle-plugins/org.shipkit.my-sample.properties does not point to the expected class'
        'anotherKey=value'                                      | 'implementation-class property not set'
    }

    def "validate missing properties file"() {
        given:
        project.file(META_INF_GRADLE_PLUGINS).mkdirs()
        project.file(PLUGIN_PACKAGE).mkdirs()
        def pluginFile = project.file("$PLUGIN_PACKAGE/TestPlugin.java") << "some content"
        def pluginFile2 = project.file("$PLUGIN_PACKAGE/AnotherTestPlugin.java") << "some content"
        Set pluginFiles = [pluginFile, pluginFile2]
        Set propertiesFiles = []

        when:
        new PluginValidator().validate(pluginFiles, propertiesFiles)
        then:
        RuntimeException ex = thrown()
        ex.message.contains 'no properties file found for plugin(s):'
        ex.message.contains "\'Test\' ($pluginFile)"
        ex.message.contains "\'AnotherTest\' ($pluginFile2)"
    }

    def "validate missing properties file not matching"() {
        given:
        project.file(META_INF_GRADLE_PLUGINS).mkdirs()
        project.file(PLUGIN_PACKAGE).mkdirs()
        Set propertiesFiles = [project.file("$META_INF_GRADLE_PLUGINS/org.sipkit.test2.properties") << "implementation-class=org.shipkit.gradle.TestPlugin"]
        def pluginFile = project.file("$PLUGIN_PACKAGE/TestPlugin.java") << "some content"
        Set pluginFiles = [pluginFile]

        when:
        new PluginValidator().validate(pluginFiles, propertiesFiles)
        then:
        RuntimeException ex = thrown()
        ex.message.contains 'no properties file found for plugin(s):'
        ex.message.contains "\'Test\' ($pluginFile)"
    }

}
