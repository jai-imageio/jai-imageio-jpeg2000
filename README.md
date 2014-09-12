jai-imageio-jpeg2000
====================

[![Build Status](https://travis-ci.org/stain/jai-imageio-jpeg2000.svg)](https://travis-ci.org/stain/jai-imageio-jpeg2000)

JPEG2000 support for Java Advanced Imaging Image I/O Tools API core
[jai-imagecore-core](https://github.com/stain/jai-imageio-core).

The `jj2000` package in this module is licensed under the
[JJ2000 license](LICENSE-JJ2000.txt) and is therefore
[not compatible with the GPL 3 license](https://github.com/stain/jai-imageio-core/issues/4).
It should however still be compatible with licenses that allow
replacable binary dependencies, like Apache, BSD and LGPL.

NOTE: This is a module extracted from the
[java.net project jai-imageio-core](https://java.net/projects/jai-imageio-core/).
It depends on the [jai-imageio-core](https://github.com/stain/jai-imageio-core)
module.

There is **NO FURTHER DEVELOPMENT** in this repository; any commits here are
just to keep the build working with recent versions of Maven/Java - the
date in the version number indicates the time of such modifications
and should not have any effect on functionality.

If you are not concerned about GPL compatibility or source code
availability, you might instead want to use
https://github.com/geosolutions-it/imageio-ext/ which is actively
maintained and extends the original imageio with many useful features,
but depends on the
[binary distribution of jai_core](http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/).


Usage
-----

To build this project, use Apache Maven 2.0.9 or newer and run:

    mvn clean install

To use jai-imageio-core-jpeg2000 from a Maven project, add:

    <dependency>
        <groupId>net.java.dev.jai-imageio</groupId>
        <artifactId>jai-imageio-jpeg2000</artifactId>
        <version>1.2-pre-dr-b04-2014-09-12</version>
    </dependency>

and:

    <repositories>
        <repository>
            <releases />
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <id>mygrid-repository</id>
            <name>myGrid Repository</name>
            <url>http://www.mygrid.org.uk/maven/repository</url>
        </repository>
    </repositories>

This repository includes source JARs and javadoc, which should be picked
up for instance by the Eclipse Maven support.

Standalone [Javadoc for jai-imageio-core](http://stain.github.io/jai-imageio-core/javadoc/) is
provided separately.


Copyright and licenses
----------------------

* Copyright © 1999/2000 JJ2000 Partners
* Copyright © 2005 Sun Microsystems
* Copyright © 2010-2014 University of Manchester

The complete copyright notice for this project is in
[COPYRIGHT.md](COPYRIGHT.md)

The source code license for the
[com.sun.media](src/main/java/com/sun/media) package
and the build modifications (e.g. `pom.xml`)
are [BSD 3-clause](http://opensource.org/licenses/BSD-3-Clause),
see [LICENSE-Sun.txt](LICENSE-Sun.txt)

The [jj2000](src/main/java/jj2000) package in this module is licensed under the
[JJ2000 license](LICENSE-JJ2000.txt) which is **not compatible
with the GNU Public License (GPL)**.


Changelog
---------

* 2014-09-12 -  Separated out [JPEG 2000](https://github.com/stain/jai-imageio-core/issues/4)
      support from [jai-imageio-core](http://github.com/stain/jai-imageio-core)
      for [licensing reasons](https://github.com/stain/jai-imageio-core/issues/4)


More info
---------

* https://github.com/stain/jai-imageio-jpeg2000
* https://github.com/stain/jai-imageio-core
* http://stain.github.io/jai-imageio-core/javadoc/
* https://java.net/projects/jai-imageio-core/
* http://www.oracle.com/technetwork/java/current-142188.html
* http://download.java.net/media/jai/builds/release/1_1_3/
