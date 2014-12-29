package net.sourceforge.carbine;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.net.URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import org.dom4j.io.SAXReader;

public class TagLoader
{
    private Map tagDefs;
    private Map namedObjects;

    public TagLoader()
    {
        tagDefs = new HashMap();
        namedObjects = new HashMap();
    }

    public void addAdditionalTags(URL url)
        throws ParseException
    {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(url);
            Element root = document.getRootElement();
            
            Object result = null;
            
            for (Iterator i = root.elementIterator(); i.hasNext();) {
                Element element = (Element) i.next();
                String elementName = element.getName();

                if (elementName.equals("tag")) {
                    parseTagDefinition(element);
                } else {
                    throw new ParseException("Only <tag> tags are allowed in additional tag definition files");
                }
            }
        } catch (DocumentException e) {
            throw new ParseException(e.getMessage());
        }
    }

    public Object parse(URL url)
        throws ParseException
    {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(url);
            Element root = document.getRootElement();
            
            Object result = null;
            
            for (Iterator i = root.elementIterator(); i.hasNext();) {
                Element element = (Element) i.next();
                String elementName = element.getName();

                if (elementName.equals("tag")) {
                    parseTagDefinition(element);
                } else if (elementName.equals("named")) {
                    parseNamed(element);
                } else {
                    if (i.hasNext()) {
                        throw new ParseException("Only one tag other than <tag> or <named> is allowed and it must go at the end");
                    } else {
                        result = parseToObject(element);
                    }
                }
            }
            
            return result;
        } catch (DocumentException e) {
            throw new ParseException(e.getMessage());
        }
    }

    private void parseTagDefinition(Element element)
    {
        String name = element.attributeValue("name");
        String className = element.attributeValue("class");
        String parentMethodName = element.attributeValue("parentMethod");
        String bodyMethodName = element.attributeValue("bodyMethod");

        TagDefinition tagDef = new TagDefinition(className, parentMethodName, bodyMethodName);
        tagDefs.put(name, tagDef);
    }

    private void parseNamed(Element element)
        throws ParseException
    {
        String name = element.attributeValue("name");
        List elements = element.elements();
        if (elements.size() != 1) {
            throw new ParseException("One and only one tag allowed in a <named> tag");
        }

        Element childElement = (Element) elements.get(0);
        Object namedObject = parseToObject(childElement);
        namedObjects.put(name, namedObject);
    }

    private Object parseToObject(Element element)
        throws ParseException
    {
        try {
            String tagName = element.getName();
            TagDefinition tagDef = (TagDefinition) tagDefs.get(tagName);
            if (tagDef == null) {
                throw new ParseException("Unknown tag: " + tagName);
            }
            String className = tagDef.getClassName();
            Class clazz = Class.forName(className);
            Object o = clazz.newInstance();
            
            for (Iterator i = element.attributeIterator(); i.hasNext();) {
                Attribute attribute = (Attribute) i.next();
                String propertyName = attribute.getName();
                String propertyValue = attribute.getValue();
                invokeStringPropertySetter(o, propertyName, propertyValue);
            }

            String bodyText = element.getText();

            if (!bodyText.equals("")) {
                String bodyMethodName = tagDef.getBodyMethodName();

                if (bodyMethodName != null) {
                    invokeSingleParameterMethod(o, bodyMethodName, bodyText);
                }
            }

            for (Iterator i = element.elementIterator(); i.hasNext();) {
                Element childElement = (Element) i.next();
                String childTagName = childElement.getName();
                if (childTagName.equals("named-ref")) {
                    String name = childElement.attributeValue("name");
                    String parentMethodName = childElement.attributeValue("parentMethod");
                    if (name == null) {
                        throw new ParseException("<named-ref> tag must have a 'name' attribute");
                    }
                    if (parentMethodName == null) {
                        throw new ParseException("<named-ref> tag must have a 'parentMethod' attribute");
                    }
                    Object child = namedObjects.get(name);
                    invokeSingleParameterMethod(o, parentMethodName, child);
                } else {
                    Object child = parseToObject(childElement);
                    TagDefinition childTagDef = (TagDefinition) tagDefs.get(childTagName);
                    String parentMethodName = childTagDef.getParentMethodName();
                    invokeSingleParameterMethod(o, parentMethodName, child);
                }
            }
            
            return o;
        } catch (ClassNotFoundException e) {
            throw new ParseException(e.getMessage());
        } catch (InstantiationException e) {
            throw new ParseException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new ParseException(e.getMessage());
        }
    }

    private void invokeStringPropertySetter(Object o, String property, String value)
        throws ParseException
    {
        StringBuffer sb = new StringBuffer();

        sb.append("set");

        for (int i = 0; i < property.length(); i++) {
            char c = property.charAt(i);
            if (i == 0) {
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }

        String methodName = sb.toString();

        invokeSingleParameterMethod(o, methodName, value);
    }

    private void invokeSingleParameterMethod(Object o, String methodName, Object value)
        throws ParseException
    {
        if (methodName == null) {
            throw new ParseException("Method name has not been specified. Check for missing parentMethod.");
        }

        try {
            Class clazz = o.getClass();
            Class paramClass = value.getClass();
            Method method = getSingleParameterMethod(clazz, methodName, paramClass);
            if (method == null) {
                throw new ParseException("Couldn't find the method " + methodName + " on " + clazz);
            }

            Object[] params = new Object[1];
            params[0] = value;
            
            method.invoke(o, params);
        } catch (IllegalAccessException e) {
            throw new ParseException(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new ParseException(e.getMessage());
        }
    }

    private Method getSingleParameterMethod(Class clazz, String methodName, Class paramClass)
    {
        Method method = null;
        try {
            Class[] paramClasses = new Class[1];
            paramClasses[0] = paramClass;
            
            method = clazz.getMethod(methodName, paramClasses);
        } catch (NoSuchMethodException e) {
            Class superClass = paramClass.getSuperclass();
            if (superClass != null) {
                method = getSingleParameterMethod(clazz, methodName, superClass);
                if (method == null) {
                    Class[] interfaces = paramClass.getInterfaces();
                    for (int i = 0; i < interfaces.length && method == null; i++) {
                        method = getSingleParameterMethod(clazz, methodName, interfaces[i]);
                    }
                }
            }
        }

        return method;
    }

    private class TagDefinition
    {
        private String className;
        private String parentMethodName;
        private String bodyMethodName;

        public TagDefinition(String className, String parentMethodName, String bodyMethodName)
        {
            this.className = className;
            this.parentMethodName = parentMethodName;
            this.bodyMethodName = bodyMethodName;
        }

        public String getClassName()
        {
            return className;
        }

        public String getParentMethodName()
        {
            return parentMethodName;
        }

        public String getBodyMethodName()
        {
            return bodyMethodName;
        }
    }
}
