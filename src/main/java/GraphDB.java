import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 */
public class GraphDB {
    /**
     * The creation of a private node class, in order to store important information that accompanies every single
     * node.
     */
    private class Node {
        public long iden;
        public double lon;
        public double lat;
        public String name;
        public String actualName;
        public ArrayList<Long> neighbors;
        public Map<Long, String> streets;

        private Node(Map<String, String> newNode) {
            iden = Long.parseLong(newNode.get("id"));
            lon = Double.parseDouble(newNode.get("lon"));
            lat = Double.parseDouble(newNode.get("lat"));
            if (newNode.containsKey("name")) {
                actualName = newNode.get("name");
                name = newNode.get("name").replaceAll("[^A-Za-z0-9]", "").toLowerCase();
            } else {
                name = null;
            }

            neighbors = new ArrayList<>();
            streets = new HashMap<>();
        }
    }

    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        clean();
    }

    /**
     * The main data structures that hold important information relating different names, nodes, and locations together.
     * @variable key: A mapping of the node identifier to the Node object, after cleaning.
     * @variable totalKey: A mapping of all nodes to the Node object, before cleaning. This is important for searching
     * nodes, by which all nodes should be able to be returned regardless if they are connected by ways.
     * @variable nameKey: A mapping of the cleaned name to the actual name for every node.
     * @variable locationKey: A mapping of the cleaned name to a list of possible node locations that correspond to that
     * name.
     */
    private Map<Long, Node> key = new HashMap<>();
    private Map<Long, Node> totalKey = new HashMap<>();
    public Map<String, String> nameKey = new HashMap<>();
    public Map<String, List<Long>> locationKey= new HashMap<>();

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    public void addNode(Map<String, String> input) {
        Node node = new Node(input);
        key.put(node.iden, node);
        totalKey.put(node.iden, node);

        if (node.name != null) {
            nameKey.put(node.name, node.actualName);

            if (!locationKey.keySet().contains(node.name)) {
                List<Long> locations = new ArrayList<>();
                locations.add(node.iden);
                locationKey.put(node.name, locations);
            } else {
                List<Long> locations = locationKey.get(node.name);
                locations.add(node.iden);
            }
        }

    }

    public void addEdge(String input1, String input2, String name) {
        long iden1 = Long.parseLong(input1);
        long iden2 = Long.parseLong(input2);

        key.get(iden1).neighbors.add(iden2);
        key.get(iden2).neighbors.add(iden1);

        key.get(iden1).streets.put(iden2, name);
        key.get(iden2).streets.put(iden1, name);
    }

    public void removeNode(String v) {
        long w = Long.parseLong(v);
        key.remove(w);
    }


    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        Set<Long> keys = new HashSet<>(key.keySet());
        for (long i : keys) {
            if (key.get(i).neighbors.size() == 0) {
                key.remove(i);
            }
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return key.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return key.get(v).neighbors;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        long closest = 0;
        double distance = Double.MAX_VALUE;
        for (Node i : key.values()) {
            if (distance(lon, lat, i.lon, i.lat) < distance) {
                distance = distance(lon, lat, i.lon, i.lat);
                closest = i.iden;
            }
        }
        return closest;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return totalKey.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return totalKey.get(v).lat;
    }

    Map<Long, String> getStreets(long v) {
        return key.get(v).streets;
    }

    List<Long> getNeighbors(long v) {
        return key.get(v).neighbors;
    }

    String getName(long v) {
        return totalKey.get(v).name;
    }
}
