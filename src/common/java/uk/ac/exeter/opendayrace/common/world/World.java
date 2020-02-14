package uk.ac.exeter.opendayrace.common.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class World {
    private final Node[] nodes;

    public World(Node[] nodes) {
        this.nodes = nodes;
    }

    public static World read(Path path) throws IOException, WorldParseException {
        Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8);
        try {
            List<NodeBuilder> builders = lines.map(NodeBuilder::new).collect(Collectors.toList());
            Node[] nodes = new Node[builders.size()];
            for (int index = 0; index < nodes.length; index++) {
                nodes[index] = builders.get(index).node;
            }
            for (NodeBuilder builder : builders) {
                builder.build(nodes);
            }
            return new World(nodes);
        } catch (UncheckedWorldParseException e) {
            throw e.getChecked();
        }
    }

    public Node[] getNodes() { return nodes; }

    private static class NodeBuilder {
        private final Node node;
        private final int[] connections;

        private String data;
        private int curr;

        private NodeBuilder(String data) {
            try {
                this.data = data;
                curr = 0;
                node = new Node(nextDouble("x1"), nextDouble("y1"),
                        nextDouble("x2"), nextDouble("y2"));
                String[] rawConns = data.substring(curr).split(",");
                connections = new int[rawConns.length];
                    for (int index = 0; index < rawConns.length; index++) {
                        connections[index] = Integer.parseInt(rawConns[index]);
                    }
            } catch (NumberFormatException e) {
                throw new UncheckedWorldParseException(
                        new WorldParseException("invalid connected node indexes in node data: " + data, e));
            } catch (WorldParseException e) {
                throw new UncheckedWorldParseException(e);
            }
        }

        private double nextDouble(String coord) throws WorldParseException {
            try {
                int next = data.indexOf(',', curr);
                if (next == -1) throw new WorldParseException("invalid node data: " + data);
                double result = Double.parseDouble(data.substring(curr, next));
                curr = next + 1;
                return result;
            } catch (NumberFormatException e) {
                throw new WorldParseException("invalid " + coord + " coordinate in node data: " + data, e);
            }
        }

        private void build(Node[] nodes) throws WorldParseException {
            try {
                for (int connection : connections) {
                    node.addConnection(nodes[connection]);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // Manually checking bounds all the time is too painful when the JVM will check them for us
                throw new WorldParseException("connected node index is out of bounds", e);
            }
        }
    }
}
