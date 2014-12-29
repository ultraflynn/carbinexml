package net.sourceforge.carbine;

import java.io.File;

import java.net.URL;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

public class TagLoaderTest extends TestCase
{
    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }

    public TagLoaderTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(TagLoaderTest.class);
    }

    private TagLoader loader;

    public void setUp()
        throws Exception
    {
        loader = new TagLoader();
    }

    public void testRootLevelOnly()
        throws Exception
    {
        File file = new File("build/classes/test/net/sourceforge/carbine/test1.xml");
        URL url = file.toURL();
        Object o = loader.parse(url);
        Assert.assertTrue(o instanceof Root);

        Root root = (Root) o;

        Assert.assertEquals("VALUE1", root.getAttr1());
        Assert.assertEquals("VALUE2", root.getAttr2());
    }

    public void testAddChild()
        throws Exception
    {
        File file = new File("build/classes/test/net/sourceforge/carbine/test2.xml");
        URL url = file.toURL();
        Object o = loader.parse(url);
        checkTest2Structure(o);
    }

    public void testNamedRef()
        throws Exception
    {
        File file = new File("build/classes/test/net/sourceforge/carbine/test3.xml");
        URL url = file.toURL();
        Object o = loader.parse(url);
        Assert.assertTrue(o instanceof SplitRoot);

        SplitRoot root = (SplitRoot) o;

        Assert.assertEquals("VALUE1", root.getAttr1());
        Assert.assertEquals("VALUE2", root.getAttr2());

        Child child1 = root.getChild1();
        Assert.assertEquals("CHILD1", child1.getName());

        Child child2 = root.getChild2();
        Assert.assertEquals("CHILD2", child2.getName());

        Root alternate = root.getAlternate();
        Assert.assertTrue(alternate != null);

        Collection children = alternate.getChildren();
        Assert.assertEquals(2, children.size());

        Iterator i = children.iterator();

        Child childA = (Child) i.next();
        Assert.assertEquals("CHILD1", childA.getName());

        Child childB = (Child) i.next();
        Assert.assertEquals("CHILD3", childB.getName());
    }

    public void testBodyLoad()
        throws Exception
    {
        File file = new File("build/classes/test/net/sourceforge/carbine/test4.xml");
        URL url = file.toURL();
        Object o = loader.parse(url);
        Assert.assertTrue(o instanceof BodyRoot);

        BodyRoot root = (BodyRoot) o;

        Assert.assertEquals("BODY TEXT", root.getBody());
    }

    public void testBodyLoadFromChild()
        throws Exception
    {
        File file = new File("build/classes/test/net/sourceforge/carbine/test5.xml");
        URL url = file.toURL();
        Object o = loader.parse(url);
        Assert.assertTrue(o instanceof Root);

        Root root = (Root) o;
        Collection children = root.getChildren();
        Assert.assertEquals(1, children.size());

        Iterator i = children.iterator();

        BodyChild child = (BodyChild) i.next();
        Assert.assertEquals("CHILD BODY", child.getBody());
    }

    public void testAdditionalTagsTest()
        throws Exception
    {
        File additional = new File("build/classes/test/net/sourceforge/carbine/test6-additional.xml");
        URL additionalUrl = additional.toURL();
        loader.addAdditionalTags(additionalUrl);

        File file = new File("build/classes/test/net/sourceforge/carbine/test6.xml");
        URL fileUrl = file.toURL();
        Object o = loader.parse(fileUrl);
        checkTest2Structure(o);
    }

    private void checkTest2Structure(Object o)
        throws Exception
    {
        Assert.assertTrue(o instanceof Root);

        Root root = (Root) o;

        Assert.assertEquals("VALUE1", root.getAttr1());
        Assert.assertEquals("VALUE2", root.getAttr2());

        Collection children = root.getChildren();
        Assert.assertEquals(2, children.size());

        Iterator i = children.iterator();

        Child child1 = (Child) i.next();
        Assert.assertEquals("CHILD1", child1.getName());

        Child child2 = (Child) i.next();
        Assert.assertEquals("CHILD2", child2.getName());
    }
}
