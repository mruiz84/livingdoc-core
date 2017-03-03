package info.novatec.testit.livingdoc.maven.plugin;

import info.novatec.testit.livingdoc.repository.AtlassianRepository;
import info.novatec.testit.livingdoc.runner.CommandLineRunner;
import info.novatec.testit.livingdoc.util.URIUtil;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.xmlrpc.WebServer;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;
import org.junit.*;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static info.novatec.testit.livingdoc.util.CollectionUtil.toVector;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SpecificationRunnerMojo.class, DynamicCoreInvoker.class, CommandLineRunner.class})
public class SpecificationRunnerMojoConfluenceTest {

    @Rule
    public MojoRule rule = new MojoRule();

    private SpecificationRunnerMojo mojo;
    private WebServer ws;
    private org.jmock.Mock handler;


    @Before
    public void setUp() throws Exception {

        URL pomPath = SpecificationRunnerMojoTest.class.getResource("pom-runner.xml");
        mojo = (SpecificationRunnerMojo) this.rule.lookupMojo("run", URIUtil.decoded(pomPath.getPath()));

        mojo.classpathElements = new ArrayList<String>();
        String core = dependency("livingdoc-core.jar").getAbsolutePath();
        mojo.classpathElements.add(core);
        mojo.classpathElements.add(dependency("guice-1.0.jar").getAbsolutePath());

        mojo.pluginDependencies = new ArrayList<Artifact>();
        mojo.pluginDependencies.add(new DependencyArtifact("commons-codec", dependency("commons-codec-1.3.jar")));
        mojo.pluginDependencies.add(new DependencyArtifact("xmlrpc", dependency("xmlrpc-2.0.1.jar")));

        Assert.assertEquals("en", mojo.locale);
        Assert.assertEquals(MySelector.class.getName(), mojo.selector);
        Assert.assertTrue(mojo.debug);
    }

    @After
    public void tearDown() {
        stopWebServer();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testShouldSupportCustomRepositoriesSuchAsConfluence() throws Exception {

        startWebServer();

        List<?> expected = toVector("SPACE", "PAGE", Boolean.TRUE, Boolean.TRUE);
        String right = FileUtils.readFileToString(spec("spec.html"), "UTF-8");
        handler.expects(new InvokeOnceMatcher()).method("getRenderedSpecification").with(eq(""), eq(""), eq(expected)).will(
                new ReturnStub(right));

        createAtlassianRepository("repo").addTest("PAGE");
        mojo.execute();

        handler.verify();
        assertReport("PAGE.html");
    }

    private Repository createAtlassianRepository(String name) {
        Repository repository = new Repository();
        repository.setName(name);
        repository.setType(AtlassianRepository.class.getName());
        repository.setRoot("http://localhost:9005/rpc/xmlrpc?includeStyle=true&handler=livingdoc1#SPACE");
        mojo.addRepository(repository);
        return repository;
    }

    private File spec(String name) {
        return new File(URIUtil.decoded(SpecificationRunnerMojoConfluenceTest.class.getResource(name).getPath()));
    }

    private void startWebServer() {
        ws = new WebServer(9005);
        handler = new org.jmock.Mock(SpecificationRunnerMojoConfluenceTest.Handler.class);
        ws.addHandler("livingdoc1", handler.proxy());
        ws.start();
    }

    private void stopWebServer() {
        if (ws != null) {
            ws.shutdown();
        }
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

    private void assertReport(String reportName) {
        File out = reportFileFor(reportName);
        Assert.assertTrue(out.exists());
        long length = out.length();
        out.delete();
        Assert.assertTrue(length > 0);
    }

    private File reportFileFor(String input) {
        return new File(new File(mojo.reportsDirectory, "repo"), URIUtil.flatten(input));
    }

    private Constraint eq(Object o) {
        return new IsEqual(o);
    }

    public static interface Handler {

        String getRenderedSpecification(String username, String password, Vector<Object> args);
    }

}
