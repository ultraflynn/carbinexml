package net.sourceforge.carbine;

import java.util.ArrayList;
import java.util.Collection;

public class Root
{
    private String attr1;
    private String attr2;
    private Collection children;

    public Root()
    {
        children = new ArrayList();
    }

    public String getAttr1()
    {
        return attr1;
    }

    public void setAttr1(String attr1)
    {
        this.attr1 = attr1;
    }

    public String getAttr2()
    {
        return attr2;
    }

    public void setAttr2(String attr2)
    {
        this.attr2 = attr2;
    }

    public Collection getChildren()
    {
        return children;
    }

    public void addChild(Child child)
    {
        children.add(child);
    }
}
