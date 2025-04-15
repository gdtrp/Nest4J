package com.gdtrp;
import com.qunhe.util.nest.data.NestPath;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.PathParser;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SvgToNestPathWithTransform {

    public static List<NestPath> convertSvgToNestPaths(InputStream svgInputStream) throws IOException {
        List<NestPath> nestPaths = new ArrayList<>();

        // 1) Parse the SVG into a Batik Document
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        Document document = factory.createDocument(null, svgInputStream);

        // 2) Recursively process the DOM from the root element
        Element rootElement = document.getDocumentElement();

        // Start with an identity transform because at the top level we have no parent transform yet
        AffineTransform initialTransform = new AffineTransform();
        processSvgNode(rootElement, initialTransform, nestPaths);

        return nestPaths;
    }

    /**
     * Recursively process each SVG node, building up transforms and extracting shapes.
     */
    private static void processSvgNode(Node node, AffineTransform parentTransform, List<NestPath> nestPaths) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }

        Element elem = (Element) node;

        // 1) Build the cumulative transform for this element by parsing its "transform" attribute (if any)
        AffineTransform thisTransform = buildTransform(elem, parentTransform);

        // 2) Check element tag name and handle geometry
        String tagName = elem.getTagName();

        switch (tagName) {
            case "g", "svg", "title":
                // a <g> may contain nested nodes
                break;
            case "path":
                String dAttr = elem.getAttribute("d");
                if (dAttr != null && !dAttr.isEmpty()) {
                    Shape pathShape = parsePathData(dAttr);
                    // Apply the cumulative transform to that shape
                    NestPath path = shapeToNestPath(pathShape, thisTransform);
                    if (path.size() > 0) {
                        nestPaths.add(path);
                    }
                }
                break;
            case "rect":
                /*
                  <rect x="..." y="..." width="..." height="..." ... />
                  Convert it to a rectangle shape and transform it.
                 */
                Shape rectShape = parseRect(elem);
                if (rectShape != null) {
                    NestPath rectPath = shapeToNestPath(rectShape, thisTransform);
                    if (rectPath.size() > 0) {
                        nestPaths.add(rectPath);
                    }
                }
                break;
            case "circle":
                Shape circleShape = parseCircle(elem);
                if (circleShape != null) {
                    NestPath circlePath = shapeToNestPath(circleShape, thisTransform);
                    if (circlePath.size() > 0) {
                        nestPaths.add(circlePath);
                    }
                }
                break;
            case "line":
                Shape lineShape = parseLine(elem);
                if (lineShape != null) {
                    NestPath linePath = shapeToNestPath(lineShape, thisTransform);
                    if (linePath.size() > 0) {
                        nestPaths.add(linePath);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported tag: " + tagName);
            // similarly, you can add "case polygon:" or "case circle:" etc.
            // to handle other geometry
        }

        // 3) Recurse through children
        Node child = node.getFirstChild();
        while (child != null) {
            processSvgNode(child, thisTransform, nestPaths);
            child = child.getNextSibling();
        }
    }

    /**
     * Build a new transform by parsing the "transform" attribute (if any)
     * and concatenating it with the given parent transform.
     */
    private static AffineTransform buildTransform(Element elem, AffineTransform parentTransform) {
        // copy the parent transform so we don't mutate it
        AffineTransform newTransform = new AffineTransform(parentTransform);

        // get this node's "transform" attribute
        String transformStr = elem.getAttribute("transform");
        if (transformStr != null && !transformStr.isEmpty()) {
            try {
                // Let Batik parse the transform string
                // Alternatively, you could parse manually or use another approach
                // but Batik can handle typical "translate(...)", "rotate(...)" etc.
                org.apache.batik.parser.TransformListParser tListParser =
                        new org.apache.batik.parser.TransformListParser();
                final List<AffineTransform> transforms = new ArrayList<>();
                tListParser.setTransformListHandler(new org.apache.batik.parser.TransformListHandler() {
                    @Override public void startTransformList() {}
                    @Override public void matrix(float a, float b, float c, float d, float e, float f) {
                        transforms.add(new AffineTransform(a, b, c, d, e, f));
                    }
                    @Override public void rotate(float theta) {
                        transforms.add(AffineTransform.getRotateInstance(
                                Math.toRadians(theta)));
                    }
                    @Override public void rotate(float theta, float cx, float cy) {
                        transforms.add(AffineTransform.getRotateInstance(
                                Math.toRadians(theta), cx, cy));
                    }
                    @Override public void translate(float tx) {
                        transforms.add(AffineTransform.getTranslateInstance(tx, 0));
                    }
                    @Override public void translate(float tx, float ty) {
                        transforms.add(AffineTransform.getTranslateInstance(tx, ty));
                    }
                    @Override public void scale(float sx) {
                        transforms.add(AffineTransform.getScaleInstance(sx, sx));
                    }
                    @Override public void scale(float sx, float sy) {
                        transforms.add(AffineTransform.getScaleInstance(sx, sy));
                    }
                    @Override public void skewX(float skx) {
                        transforms.add(AffineTransform.getShearInstance(Math.tan(Math.toRadians(skx)), 0));
                    }
                    @Override public void skewY(float sky) {
                        transforms.add(AffineTransform.getShearInstance(0, Math.tan(Math.toRadians(sky))));
                    }
                    @Override public void endTransformList() {}
                });
                tListParser.parse(transformStr);

                // Combine all transforms in the order they appear
                for (AffineTransform atf : transforms) {
                    newTransform.concatenate(atf);
                }

            } catch (Exception e) {
                e.printStackTrace();
                // if transform parse fails, we just ignore or handle as needed
            }
        }

        return newTransform;
    }

    /**
     * Parse an SVG <path d="..."> string into a Java AWT Shape using Batik.
     */
    private static Shape parsePathData(String pathData) {
        PathParser pathParser = new PathParser();
        AWTPathProducer producer = new AWTPathProducer();
        producer.setWindingRule(PathIterator.WIND_NON_ZERO);
        pathParser.setPathHandler(producer);
        pathParser.parse(pathData);
        return producer.getShape();
    }

    private static Shape parseCircle(Element circleElem) {
        try {
            double cx = Double.parseDouble(circleElem.getAttribute("cx"));
            double cy = Double.parseDouble(circleElem.getAttribute("cy"));
            double r  = Double.parseDouble(circleElem.getAttribute("r"));
            // Ellipse2D for the bounding box (cx-r, cy-r, 2r, 2r)
            return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        } catch (NumberFormatException e) {
            // Possibly missing or invalid attributes
            return null;
        }
    }
    private static Shape parseLine(Element lineElem) {
        try {
            double x1 = Double.parseDouble(lineElem.getAttribute("x1"));
            double y1 = Double.parseDouble(lineElem.getAttribute("y1"));
            double x2 = Double.parseDouble(lineElem.getAttribute("x2"));
            double y2 = Double.parseDouble(lineElem.getAttribute("y2"));
            java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            return path;
        } catch (NumberFormatException e) {
            return null; // Skip invalid lines
        }
    }
    /**
     * Parse an SVG <rect> element into an AWT Shape (Rectangle2D).
     */
    private static Shape parseRect(Element rectElem) {
        try {
            double x = Double.parseDouble(rectElem.getAttribute("x"));
            double y = Double.parseDouble(rectElem.getAttribute("y"));
            double w = Double.parseDouble(rectElem.getAttribute("width"));
            double h = Double.parseDouble(rectElem.getAttribute("height"));
            return new Rectangle2D.Double(x, y, w, h);
        } catch (NumberFormatException e) {
            // If attributes are missing or invalid, skip
            return null;
        }
    }

    /**
     * Convert an AWT Shape to a NestPath, applying the specified AffineTransform
     * while flattening the shape into line segments.
     */
    private static NestPath shapeToNestPath(Shape shape, AffineTransform transform) {
        NestPath nestPath = new NestPath();

        // Flatten the shape with a "flatness" parameter (the smaller, the finer the approximation)
        // Passing 'transform' in getPathIterator applies the accumulated transformations.
        PathIterator pathIterator = shape.getPathIterator(transform, 0.1);
        double[] coords = new double[6];

        while (!pathIterator.isDone()) {
            int segmentType = pathIterator.currentSegment(coords);
            // MoveTo or LineTo => store the point
            if (segmentType == PathIterator.SEG_MOVETO || segmentType == PathIterator.SEG_LINETO) {
                nestPath.add(coords[0], coords[1]);
            }
            pathIterator.next();
        }

        return nestPath;
    }

}