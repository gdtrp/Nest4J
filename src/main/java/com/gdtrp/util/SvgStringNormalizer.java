package com.gdtrp.util;

import com.gdtrp.SvgToNestPathWithTransform;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SvgStringNormalizer {
    public static String normalizeSvgWithRewrittenCoordinates(String svgString) throws Exception {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        Document doc = factory.createDocument(null, new StringReader(svgString));
        Element root = doc.getDocumentElement();

        // Compute bounding box with all transforms applied
        List<PathElement> allPoints = new ArrayList<>();
        collectPathPoints(root, new AffineTransform(), allPoints);

        double minX = allPoints.stream().mapToDouble(p -> p.x).min().orElse(0);
        double minY = allPoints.stream().mapToDouble(p -> p.y).min().orElse(0);

        // Shift all path coordinates
        if (minX < 0 || minY < 0) {
            rewriteAllPaths(root, new AffineTransform(), -minX, -minY);
        }

        // Remove all transform attributes — they're now baked in
        removeTransforms(root);

        return domToString(doc);
    }

    private static void collectPathPoints(Node node, AffineTransform parentTx, List<PathElement> out) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) node;
            AffineTransform tx = SvgToNestPathWithTransform.buildTransform(el, parentTx);

            if ("path".equals(el.getTagName()) && el.hasAttribute("d")) {
                Shape shape = SvgToNestPathWithTransform.parsePathData(el.getAttribute("d"));
                PathIterator it = shape.getPathIterator(tx, 0.1);
                double[] coords = new double[6];
                while (!it.isDone()) {
                    int type = it.currentSegment(coords);
                    if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                        out.add(new PathElement(coords[0], coords[1]));
                    }
                    it.next();
                }
            }

            NodeList children = el.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                collectPathPoints(children.item(i), tx, out);
            }
        }
    }

    private static void rewriteAllPaths(Node node, AffineTransform parentTx, double dx, double dy) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) node;
            AffineTransform tx = SvgToNestPathWithTransform.buildTransform(el, parentTx);

            if ("path".equals(el.getTagName()) && el.hasAttribute("d")) {
                Shape shape = SvgToNestPathWithTransform.parsePathData(el.getAttribute("d"));
                PathIterator it = shape.getPathIterator(tx, 0.1);
                double[] coords = new double[6];
                StringBuilder newD = new StringBuilder();

                boolean first = true;
                while (!it.isDone()) {
                    int type = it.currentSegment(coords);
                    if (type == PathIterator.SEG_MOVETO) {
                        newD.append("M ").append(format(coords[0] + dx)).append(",").append(format(coords[1] + dy)).append(" ");
                    } else if (type == PathIterator.SEG_LINETO) {
                        newD.append("L ").append(format(coords[0] + dx)).append(",").append(format(coords[1] + dy)).append(" ");
                    } else if (type == PathIterator.SEG_CLOSE) {
                        newD.append("Z ");
                    }
                    it.next();
                }

                el.setAttribute("d", newD.toString().trim());
            }

            NodeList children = el.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                rewriteAllPaths(children.item(i), tx, dx, dy);
            }
        }
    }

    private static void removeTransforms(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            ((Element) node).removeAttribute("transform");
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                removeTransforms(children.item(i));
            }
        }
    }

    private static String format(double val) {
        return String.format(Locale.US, "%.4f", val);
    }

    private static String domToString(Document doc) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        tf.setOutputProperty(OutputKeys.METHOD, "xml");
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private static class PathElement {
        double x, y;

        PathElement(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
