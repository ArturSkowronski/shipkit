## Getting started

Thank you for using Shipkit!
See also [documentation index](../README.md#documentation)

Please help us with the documentation.
Pull requests are very welcome!

To quickly learn how Shipkit works you can test drive this guide using our [Shipkit Bootstrap project](https://github.com/mockito/shipkit-bootstrap).
Otherwise, apply below steps to your own project:

### Adding dependency

Add below to your root "build.gradle" file.
Get the latest version of "org.shipkit.java" plugin from [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.shipkit.java)

```Gradle
plugins {
    id "org.shipkit.java" version "TODO"
}

apply plugin: "org.shipkit.java"
```

For advanced users, if you need to use the traditional way of configuring Gradle plugins:

```Gradle
buildscript {
    repositories {
        maven { url "https://plugins.gradle.org/m2/" }
    }

    dependencies {
        classpath "org.shipkit:shipkit:0.9.73"
    }
}

apply plugin: "org.shipkit.java"
```

### Initializing Shipkit

When you are done you can open the terminal and in your project’s root directory run the following command:

    ./gradlew initShipkit

This will generate all configuration files that you need.

* version.properties — it has the information about version of your project.
From now on you should have version specified only here, instead of in any of the Gradle configuration files.
The default value is based on your current version of the project, taken from Gradle project.version or “0.0.1” if unspecified.
You can override it yourself by changing ‘version’ property manually.

* shipkit.gradle — it’s a place for all configuration properties (GitHub repo, Bintray etc).
It is filled with examplary values and a few may be kept but most of them you need to change by yourself.

* .travis.yml — this file is needed by Travis and it specifies what kind of build logic will be executed there.

### Shipkit.gradle configuration file

At the beginning of shipkit.gradle you can find shipkit extension:

```Gradle
shipkit {
    gitHub.repository = "wwilk/shipkit-demo"
    gitHub.readOnlyAuthToken = "e7fe8fcfd6ffedac384c8c4c71b2a48e646"
}
```

Property **github.repository** is by default filled with your remote origin URL, while **github.readOnlyAuthToken** is set to the token for generic [shipkit-org](https://github.com/shipkit-org) account.
It is sufficient to test release locally.
To perform write operations (like git push) you would need a write token but if you don't specify it Shipkit will use your local GitHub authentication.
GitHub configuration is covered in more detail later in this document.

### Bintray configuration

Bintray configuration of the generated shipkit.gradle looks like this:

```Gradle
allprojects {
    plugins.withId("org.shipkit.bintray") {
        bintray {
            key = '7ea297848ca948adb7d3ee92a83292112d7ae989'
            pkg {
                repo = 'bootstrap'
                user = 'shipkit-bootstrap-bot'
                userOrg = 'shipkit-bootstrap'
                name = 'maven'
                licenses = ['MIT']
                labels = ['continuous delivery', 'release automation', 'shipkit']
            }
        }
    }
}
```

To only play around with Shipkit you don’t need to change any Bintray configuration!
Shipkit maintains shipkit-bootstrap Bintray organisation for you to try on Shipkit publishing without even creating a Bintray account!
This repository's settings are the default values in shipkit.gradle file.

### Test run

Now without changing any configuration file you can do a Shipkit release from your command line. For this run:

    ./gradlew performRelease

It will use your local GitHub authentication, Shipkit Bootstrap Bintray repository to:

- increment the project's version
- update release notes
- create a new version tag
- commit and push changes
- upload artifacts to Shipkit Bootstrap

### Production configuration

When you are done testing Shipkit and you want to use it on production there is only a couple of things you need to do.

#### GitHub

You need to [generate personal access tokens on GitHub](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/).
Please generate 1 read-only token and 1 write token.
When generating the token, GitHub asks about "auth scopes".
Read-only token should have no auth scopes checked.
Write token needs "public repo" auth token checked.
Read-only token will be checked in with the source code and will enable any contributor to perform light release testing (preview release notes, automatically generate contributors for poms, etc).
Write token should be used on CI machine to perform actual releases.
Export write token as **GH_WRITE_TOKEN** env variable on Travis CI.
We recommend configuring env variables in [Travis CI repository settings](https://docs.travis-ci.com/user/environment-variables/#Defining-Variables-in-Repository-Settings).

#### Shipkit

There is a lot of other properties that you can change in shipkit extension.
You can find them [here](https://github.com/mockito/shipkit/blob/master/subprojects/shipkit/src/main/groovy/org/shipkit/gradle/configuration/ShipkitConfiguration.java).

#### Bintray

You need to change default values in shipkit.gradle to the appropriate ones for your project.
For this you have to sign up for Bintray's free open source plan, and at least one repository.
See Bintray's [getting started guide](https://bintray.com/docs/usermanual/starting/starting_gettingstarted.html).

Once you created Bintray account, please generate [Bintray API key](https://bintray.com/docs/usermanual/interacting/interacting_interacting.html#anchorAPIKEY) so that you can publish automatically to your repositories.
For safety, don't check in the API key to Git.
Instead, configure **BINTRAY_API_KEY** environment variable in [Travis CI repository settings](https://docs.travis-ci.com/user/environment-variables/#Defining-Variables-in-Repository-Settings).

Finally, configure few other Bintray settings in "shipkit.gradle" file, like repo name, user name, etc.
Shipkit uses Gradle Bintray Plugin underneath and its Bintray extension, so you [can check the documentation](https://github.com/bintray/gradle-bintray-plugin) to get more details and other properties you can configure.

#### Travis configuration

You can sign in to Travis with your GitHub account, and then you need to enable your project to be built on Travis.
Here is how generated (by running initShipkit task) ".travis.yml" file looks like:

```yml
# More details on how to configure the Travis build
# https://docs.travis-ci.com/user/customizing-the-build/

# Speed up build with travis caches
cache:
  directories:
    - $HOME/.gradle/caches/
    - $HOME/.gradle/wrapper/

language: java

jdk:
  - oraclejdk8

#don't build tags
branches:
  except:
  - /^v\d/

#Build and perform release (if needed)
script:
 - ./gradlew build -s && ./gradlew ciPerformRelease
```

The "script" part of Travis CI setup consists of 2 operations separate with "&&". This is the easiest way to configure releases in Travis. The first operation is typically the "build" command, but you totally configure it. Second operation uses "&&" so that it is only triggered if the build succeeds. "ciPerformRelease" task is the core of Shipkit, it aggregates few other tasks:

- **assertReleaseNeeded** which checks if release should be made during this build.
There is a number of ways how you can skip release — eg. by using **[ci skip-release]** in your commit message or set **SKIP_RELEASE** environment variable.
You can find more ways here.
- **ciReleasePrepare** that sets up Git configuration on Travis, eg. sets git.user and git.email properties
- **performRelease** which, among others, bumps PATCH part of the semantic version in your version.properties, updates release notes and publishes artifacts to Bintray

This is it! Shipkit is now configured in your project.
You can try it by pushing anything to master, which should start the Travis build.

### Reference projects

Sometimes it is best to learn from real projects.
You can view "shipkit.gradle", ".travis.yml" and "build.gradle" of projects that already use Shipkit:

 - [Shipkit Example](https://github.com/mockito/shipkit-example) - small and simple example, best to get started
 - [Mockito](https://github.com/mockito/mockito) - real project with various interesting Shipkit customizations
 - [Powermock](https://github.com/powermock/powermock) - another real project
 - [Shipkit Bootstrap project](https://github.com/mockito/shipkit-bootstrap) - use it to learn Shipkit quickly by test driving this user guide.
 Bootstrap project does not use Shipkit until you make it so :)

