package org.jgroups.util;

import org.jgroups.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Extracts version and codename from pom.xml and writes them to ./classes/JGROUPS_VERSION.properties
 * @author Bela Ban
 * @since  5.0.0
 */
public class WriteVersionTo {
    public static void main(String[] args) throws IOException {
        String pom="pom.xml";
        String input="./conf/JGROUPS_VERSION.properties";
        String output="./classes/JGROUPS_VERSION.properties";
        String jgroups_version="jgroups.version", jgroups_codename="jgroups.codename";

        for(int i=0; i < args.length; i++) {
            switch(args[i]) {
                case "-pom":
                    pom=args[++i];
                    break;
                case "-input":
                    input=args[++i];
                    break;
                case "-output":
                    output=args[++i];
                    break;
                case "-version":
                    jgroups_version=args[++i];
                    break;
                case "-codename":
                    jgroups_codename=args[++i];
                    break;
                default:
                    System.out.printf("%s [-pom POM] [-input <input file>] [-output <output file>] " +
                                        "[-version <version name>] [-codename <codename>]\n",
                                      WriteVersionTo.class.getSimpleName());
                    return;
            }
        }
        InputStream in=Util.getResourceAsStream(input, null);
        if(in == null)
            in=new FileInputStream(input);
        if(in == null)
            throw new FileNotFoundException(input);

        Properties props=new Properties();
        props.load(in);

        String[] result=parseVersionAndCodenameFromPOM(pom);
        String version=result[0], codename=result[1];
        props.replace(jgroups_version, version);
        props.replace(jgroups_codename, codename);

        System.out.println("props = " + props);
        try(OutputStream out=new FileOutputStream(output)) {
            props.store(out, "Generated by " + WriteVersionTo.class.getSimpleName());
        }
    }


    public static String[] parseVersionAndCodenameFromPOM(String pom) throws IOException {
        String version, codename;
        try(InputStream in=Util.getResourceAsStream(pom, Version.class)) {
            if(in == null)
                throw new FileNotFoundException(pom);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            final AtomicReference<SAXParseException> ex = new AtomicReference<>();
            builder.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXException {
                    System.err.printf(Util.getMessage("ParseFailure"), e);
                }
                public void fatalError(SAXParseException exception) throws SAXException {ex.set(exception);}
                public void error(SAXParseException exception) throws SAXException {ex.set(exception);}
            });
            Document document = builder.parse(in);
            if(ex.get() != null)
                throw ex.get();
            Element root_element=document.getDocumentElement();
            String root_name=root_element.getNodeName().trim().toLowerCase();
            if(!"project".equals(root_name))
                throw new IOException("the POM does not start with a <project> element: " + root_name);
            version=Util.getChild(root_element, "version");
            codename=Util.getChild(root_element, "properties.codename");
            return new String[]{version, codename};
        }
        catch (Exception x) {
            throw new IOException(String.format(Util.getMessage("ParseError"), x.getLocalizedMessage()));
        }
    }



}