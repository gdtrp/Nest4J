package com.gdtrp;

import com.qunhe.util.nest.data.NestPath;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.PathParser;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SvgToNestPathConverter {

    /**
     * Parse an SVG input stream and convert all <path> elements to a list of NestPath objects.
     *
     * @param svgInputStream InputStream of the SVG file
     * @return List of NestPath objects
     * @throws IOException if there's a problem reading the SVG
     */
    public static List<NestPath> convertSvgToNestPaths(InputStream svgInputStream) throws IOException {
        List<NestPath> nestPaths = new ArrayList<>();

        // Create an SVG Document
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        Document document = factory.createDocument(null, svgInputStream);

        // Get all <path> elements in the document
        NodeList pathNodes = document.getElementsByTagName("path");

        // For each path element, parse the 'd' attribute and convert to NestPath
        for (int i = 0; i < pathNodes.getLength(); i++) {
            Element pathElement = (Element) pathNodes.item(i);
            String dAttr = pathElement.getAttribute("d");
            if (dAttr == null || dAttr.trim().isEmpty()) {
                continue;
            }

            // Parse the path data into a Java AWT Shape
            Shape awtShape = parsePathData(dAttr);

            // Convert the shape into a NestPath
            NestPath nestPath = shapeToNestPath(awtShape);
            if (nestPath.size() > 0) {
                nestPaths.add(nestPath);
            }
        }

        // You could also parse <polygon>, <polyline>, <rect>, <circle>, etc. similarly
        // if your SVG contains other geometry shapes.

        return nestPaths;
    }

    /**
     * Use the Batik PathParser to convert the SVG path data (the 'd' attribute) into a Java AWT Shape.
     *
     * @param pathData The 'd' attribute of the path
     * @return AWT Shape representing the path
     */
    private static Shape parsePathData(String pathData) {
        PathParser pathParser = new PathParser();
        AWTPathProducer producer = new AWTPathProducer();
        producer.setWindingRule(PathIterator.WIND_NON_ZERO);
        pathParser.setPathHandler(producer);
        pathParser.parse(pathData);
        return producer.getShape();
    }

    /**
     * Convert a Java AWT Shape into a Nest4J NestPath.
     * We step through its PathIterator, extract the coordinates,
     * and build a polygon.
     *
     * Note: This does a naive polygon extraction. Curves, arcs, etc.
     *       are flattened by the PathIterator.
     *
     * @param shape AWT Shape to convert
     * @return NestPath instance containing polygon points
     */
    private static NestPath shapeToNestPath(Shape shape) {
        NestPath nestPath = new NestPath();

        // Flatten the path into line segments by specifying a small flatness
        PathIterator pathIterator = shape.getPathIterator(null, 0.1);
        double[] coords = new double[6];

        while (!pathIterator.isDone()) {
            int segmentType = pathIterator.currentSegment(coords);

            // MoveTo or LineTo => store the point
            if (segmentType == PathIterator.SEG_MOVETO || segmentType == PathIterator.SEG_LINETO) {
                double x = coords[0];
                double y = coords[1];
                nestPath.add(x, y);
            }
            // We ignore SEG_CLOSE (because it typically means close polygon).
            // If you want a closed polygon, you can check this and possibly
            // re-add the first point or handle closure as needed.

            pathIterator.next();
        }

        return nestPath;
    }

}