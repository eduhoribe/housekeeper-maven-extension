# Housekeeper Maven Extension

[![](https://jitpack.io/v/eduhoribe/housekeeper-maven-extension.svg)](https://jitpack.io/#eduhoribe/housekeeper-maven-extension)

## TL;DR;

Take a look at the [demo](https://github.com/eduhoribe/housekeeper-demo),
specially the [`.mvn`](https://github.com/eduhoribe/housekeeper-demo/blob/master/.mvn) directory

## Getting started

### Step 1: Add the JitPack repository in the `settings.xml` file

If you don't have one yet, I do recommend that you create
the [`.mvn/maven.config`](https://github.com/eduhoribe/housekeeper-demo/blob/master/.mvn/maven.config)
and [`.mvn/settings.xml`](https://github.com/eduhoribe/housekeeper-demo/blob/master/.mvn/settings.xml) files like
the [demo](https://github.com/eduhoribe/housekeeper-demo).
The final result should look like this:

```xml

<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    ...
    <profiles>
        ...
        <profile>
            <id>profile-name</id>
            ...
            <pluginRepositories>
                ...
                <pluginRepository>
                    <id>jitpack.io</id>
                    <url>https://jitpack.io</url>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>
    ...
</settings>
```

### Step 2: Add the extension

Create a `.mvn/extensions.xml` file with the housekeeper extension

```xml

<extensions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/EXTENSIONS/1.0.0"
            xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
    <extension>
        <groupId>com.github.eduhoribe</groupId>
        <artifactId>housekeeper-maven-extension</artifactId>
        <version>0.1.1</version>
    </extension>
</extensions>
```

## Usage

You can extract some sections inside the `pom.xml` file to a separated file. The current supported sections are:

| XPath                          | File                             |
|--------------------------------|----------------------------------|
| project → build → plugins      | `.mvn/plugins.xml`               |
| project → dependencyManagement | `.mvn/dependency_management.xml` |

Feel free to suggest more sections to be added.
