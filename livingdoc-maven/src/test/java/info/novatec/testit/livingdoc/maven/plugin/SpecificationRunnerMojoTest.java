/* Copyright (c) 2007 Pyxis Technologies inc.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site:
 * http://www.fsf.org. */

package info.novatec.testit.livingdoc.maven.plugin;

import info.novatec.testit.livingdoc.repository.FileSystemRepository;
import info.novatec.testit.livingdoc.util.URIUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;


public class SpecificationRunnerMojoTest extends AbstractMojoTestCase {
    private SpecificationRunnerMojo mojo;

    @Override
    protected void tearDown() throws Exception {
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        URL pomPath = SpecificationRunnerMojoTest.class.getResource("pom-runner.xml");
        mojo = (SpecificationRunnerMojo) lookupMojo("run", URIUtil.decoded(pomPath.getPath()));
        mojo.classpathElements = new ArrayList<String>();
        String core = dependency("livingdoc-core.jar").getAbsolutePath();
        mojo.classpathElements.add(core);
        mojo.classpathElements.add(dependency("guice-1.0.jar").getAbsolutePath());

        mojo.pluginDependencies = new ArrayList<Artifact>();
        mojo.pluginDependencies.add(new DependencyArtifact("commons-codec", dependency("commons-codec-1.3.jar")));
        mojo.pluginDependencies.add(new DependencyArtifact("xmlrpc", dependency("xmlrpc-2.0.1.jar")));

        assertEquals("en", mojo.locale);
        assertEquals(MySelector.class.getName(), mojo.selector);
        assertTrue(mojo.debug);
    }

    private Repository createLocalRepository(String name) {
        Repository repository = new Repository();
        repository.setName(name);
        repository.setType(FileSystemRepository.class.getName());
        repository.setRoot(localPath());
        mojo.addRepository(repository);
        return repository;
    }

    private String localPath() {
        return localDir().getAbsolutePath();
    }

    private File localDir() {
        return spec("spec.html").getParentFile();
    }

    private File dependency(String name) {
        return new File(classesOutputDir(), name);
    }

    private File classesOutputDir() {
        return localDir().getParentFile().getParentFile().getParentFile().getParentFile();
    }

    public void testCanRunASingleFileSpecification() throws Exception {
        createLocalRepository("repo").addTest("right.html");
        mojo.execute();

        assertReport("right.html");
    }

    public void testShouldSupportSpecifyingCustomSystemUnderDevelopmentSuchAsGuice() throws Exception {
        createLocalRepository("repo").addTest("guice.html");
        mojo.systemUnderDevelopment = "info.novatec.testit.livingdoc.extensions.guice.GuiceSystemUnderDevelopment";
        mojo.execute();

        assertReport("guice.html");
    }

    public void testCanRunASuiteOfSpecifications() throws Exception {
        createLocalRepository("repo").addSuite("/");
        try {
            mojo.execute();
        } catch (MojoFailureException ignored) {
            // No implementation needed.
        }
        assertReport("right.html");
        assertReport("wrong.html");
    }

    public void testSupportsMultipleRepositories() throws Exception {
        createLocalRepository("repo").addTest("right.html");
        createLocalRepository("repo").addTest("wrong.html");

        try {
            mojo.execute();
        } catch (MojoFailureException ignored) {
            // No implementation needed.
        }
        assertReport("right.html");
        assertReport("wrong.html");
    }

    public void testShouldMakeBuildFailIfThereWereTestFailures() throws Exception {
        createLocalRepository("repo").addTest("wrong.html");
        try {
            mojo.execute();
            fail();
        } catch (MojoFailureException expected) {
            assertTrue(true);
        }
        assertReport("wrong.html");
    }

    public void testShouldMakeBuildFailIfSomeTestsCouldNotBeRun() throws Exception {
        createLocalRepository("repo").addTest("no_such_file.html");
        try {
            mojo.execute();
            fail();
        } catch (MojoExecutionException expected) {
            assertTrue(true);
        }
    }

    private File reportFileFor(String input) {
        return new File(new File(mojo.reportsDirectory, "repo"), URIUtil.flatten(input));
    }

    private File spec(String name) {
        return new File(URIUtil.decoded(SpecificationRunnerMojoTest.class.getResource(name).getPath()));
    }

    private void assertReport(String reportName) {
        File out = reportFileFor(reportName);
        assertTrue(out.exists());
        long length = out.length();
        out.delete();
        assertTrue(length > 0);
    }
}

