/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.node;

import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ModelSyntaxException;

/**
 * Base class of for all Smithy model nodes.
 *
 * <p>When loading a Smithy model the data is loaded from the source model
 * file into a tree of nodes. These nodes represent the unvalidated
 * structure of the model document.
 */
public abstract class Node implements FromSourceLocation, ToNode {
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final DefaultNodeFactory NODE_FACTORY = new DefaultNodeFactory(JSON_FACTORY);
    private final SourceLocation sourceLocation;

    /**
     * This is intentionally package-private to make Node a closed set
     * of concrete classes and prevent it from being extended outside
     * of this package.
     *
     * @param sourceLocation Where the node was sourced from.
     */
    Node(SourceLocation sourceLocation) {
        this.sourceLocation = Objects.requireNonNull(sourceLocation);
    }

    /**
     * Attempts to parse the given JSON string and return a Node.
     *
     * @param json JSON text to parse.
     * @return Returns the parsed Node on success.
     * @throws ModelSyntaxException if the JSON text is invalid.
     */
    public static Node parse(String json) {
        return parse(json, "");
    }

    /**
     * Attempts to parse the given JSON string and File Name and return a Node.
     *
     * @param json JSON text to parse.
     * @param file Filename corresponding to json text
     * @return Returns the parsed Node on success.
     * @throws ModelSyntaxException if the JSON text is invalid.
     */
    public static Node parse(String json, String file) {
        return NODE_FACTORY.createNode(file, json);
    }

    /**
     * Attempts to parse the given JSON string and File Name and return a Node.
     *
     * <p>This parser allows for comments in the JSON.
     *
     * @param json JSON text to parse.
     * @param file Filename corresponding to json text
     * @return Returns the parsed Node on success.
     * @throws ModelSyntaxException if the JSON text is invalid.
     */
    public static Node parseJsonWithComments(String json, String file) {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
        return new DefaultNodeFactory(factory).createNode(file, json);
    }

    /**
     * Attempts to parse the given JSON string and return a Node.
     *
     * <p>This parser allows for comments in the JSON.
     *
     * @param json JSON text to parse.
     * @return Returns the parsed Node on success.
     * @throws ModelSyntaxException if the JSON text is invalid.
     */
    public static Node parseJsonWithComments(String json) {
        return parseJsonWithComments(json, "");
    }

    /**
     * Writes the contents of a Node to a pretty-printed JSON string.
     *
     * <p>Use {@link DefaultNodeWriter} directly to customize the output,
     * write directly to a file, etc.
     *
     * @param node Node to write.
     * @return Returns the serialized Node.
     */
    public static String prettyPrintJson(Node node) {
        StringWriter writer = new StringWriter();
        return printJson(node, createJsonParser(writer).setPrettyPrinter(new NodePrettyPrinter()), writer);
    }

    /**
     * Writes the contents of a Node to a non-pretty-printed JSON string.
     *
     * @param node Node to write.
     * @return Returns the serialized Node.
     */
    public static String printJson(Node node) {
        StringWriter writer = new StringWriter();
        return printJson(node, createJsonParser(writer), writer);
    }

    private static JsonGenerator createJsonParser(StringWriter writer) {
        try {
            return JSON_FACTORY.createGenerator(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String printJson(Node node, JsonGenerator generator, StringWriter writer) {
        try {
            DefaultNodeWriter nodeWriter = new DefaultNodeWriter(generator);
            node.accept(nodeWriter);
            generator.close();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a {@link StringNode} from a String value.
     *
     * @param value Value to create a node from.
     * @return Returns the created StringNode.
     */
    public static StringNode from(String value) {
        return new StringNode(value, SourceLocation.none());
    }

    /**
     * Create a {@link NumberNode} from a Number value.
     *
     * @param number Value to create a node from.
     * @return Returns the created NumberNode.
     */
    public static NumberNode from(Number number) {
        return new NumberNode(number, SourceLocation.none());
    }

    /**
     * Create a {@link BooleanNode} from a boolean value.
     *
     * @param value Value to create a node from.
     * @return Returns the created BooleanNode.
     */
    public static BooleanNode from(boolean value) {
        return new BooleanNode(value, SourceLocation.none());
    }

    /**
     * Creates an {@link ArrayNode} from a Collection of Node values.
     *
     * @param values String values to add to the ArrayNode.
     * @return Returns the created ArrayNode.
     */
    public static ArrayNode fromNodes(List<Node> values) {
        return new ArrayNode(values, SourceLocation.none());
    }

    /**
     * Creates an {@link ArrayNode} from a variadic list of Node values.
     *
     * @param values String values to add to the ArrayNode.
     * @return Returns the created ArrayNode.
     */
    public static ArrayNode fromNodes(Node... values) {
        return fromNodes(Arrays.asList(values));
    }

    /**
     * Creates an {@link ArrayNode} from a Collection of String values.
     *
     * @param values String values to add to the ArrayNode.
     * @return Returns the created ArrayNode.
     */
    public static ArrayNode fromStrings(Collection<String> values) {
        return fromNodes(values.stream().map(Node::from).collect(Collectors.toList()));
    }

    /**
     * Creates an {@link ArrayNode} from a variadic list of String values.
     *
     * @param values String values to add to the ArrayNode.
     * @return Returns the created ArrayNode.
     */
    public static ArrayNode fromStrings(String... values) {
        return fromStrings(Arrays.asList(values));
    }

    /**
     * Creates an {@link ObjectNode.Builder}.
     *
     * @return Returns the ObjectNode builder.
     */
    public static ObjectNode.Builder objectNodeBuilder() {
        return new ObjectNode.Builder();
    }

    /**
     * Creates an empty {@link ObjectNode}.
     * @return Returns the ObjectNode.
     */
    public static ObjectNode objectNode() {
        return ObjectNode.EMPTY;
    }

    /**
     * Creates an {@link ObjectNode} from the given map of Nodes.
     * @param values Values to add to the object node.
     * @return Returns the created ObjectNode.
     */
    public static ObjectNode objectNode(Map<StringNode, Node> values) {
        return new ObjectNode(values, SourceLocation.none());
    }

    /**
     * Creates an empty {@link ArrayNode}.
     * @return Returns the ArrayNode.
     */
    public static ArrayNode arrayNode() {
        return ArrayNode.EMPTY;
    }

    /**
     * Creates an {@link ArrayNode} from a variadic list of Nodes.
     * @param nodes Nodes to add to the array.
     * @return Returns the created ArrayNode.
     */
    public static ArrayNode arrayNode(Node... nodes) {
        return new ArrayNode(Arrays.asList(nodes), SourceLocation.none());
    }

    /**
     * Creates a {@link NullNode}.
     * @return Returns the NullNode.
     */
    public static NullNode nullNode() {
        return new NullNode(SourceLocation.none());
    }

    /**
     * Expects an array of strings and returns the loaded strings.
     *
     * @param descriptor Name of the property being loaded.
     * @param node Node to load.
     * @return Returns the loaded strings.
     * @throws SourceException on error.
     */
    public static List<String> loadArrayOfString(String descriptor, Node node) {
        return node.expectArrayNode("Expected `" + descriptor + "` to be an array of strings. Found {type}.")
                .getElements().stream()
                .map(element -> element.expectStringNode(
                        "Each element of `" + descriptor + "` must be a string. Found {type}."))
                .map(StringNode::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Testing helper used to compare two Nodes for equivalence.
     *
     * <p>Compares two Node values and throws if they aren't equal. The
     * thrown exception contains a message that shows the differences
     * between the two Nodes as returned by {@link #diff(ToNode, ToNode)}.
     *
     * @param actual Node to use as the starting node.
     * @param expected Node to compare against.
     * @throws ExpectationNotMetException if the nodes are not equivalent.
     */
    public static void assertEquals(ToNode actual, ToNode expected) {
        Node actualNode = actual.toNode();
        Node expectedNode = expected.toNode();
        if (!actualNode.equals(expectedNode)) {
            throw new ExpectationNotMetException(String.format(
                    "Actual node did not match expected Node.%nActual:%n%s%nExpected:%n%s%nDiff: %s",
                    Node.prettyPrintJson(actualNode),
                    Node.prettyPrintJson(expectedNode),
                    String.join(System.lineSeparator(), diff(actualNode, expectedNode))), actualNode);
        }
    }

    /**
     * Computes the differences between two Nodes as a String.
     *
     * @param actual Node to use as the starting node.
     * @param expected Node to compare against.
     * @return Returns the differences as a String.
     */
    public static List<String> diff(ToNode actual, ToNode expected) {
        return NodeDiff.diff(actual, expected);
    }

    /**
     * Gets the type of the node.
     *
     * @return Returns the node type.
     */
    public abstract NodeType getType();

    /**
     * Accepts a visitor with the node.
     *
     * @param visitor Visitor to dispatch to.
     * @param <R> visitor return type.
     * @return Returns the accepted result.
     */
    public abstract <R> R accept(NodeVisitor<R> visitor);

    /**
     * Checks if this node is an object type.
     *
     * @return Returns true if this node is an object type.
     */
    public final boolean isObjectNode() {
        return getType() == NodeType.OBJECT;
    }

    /**
     * Checks if this node is an array type.
     *
     * @return Returns true if this node is an array type.
     */
    public final boolean isArrayNode() {
        return getType() == NodeType.ARRAY;
    }

    /**
     * Checks if this node is a string type.
     *
     * @return Returns true if this node is a string type.
     */
    public final boolean isStringNode() {
        return getType() == NodeType.STRING;
    }

    /**
     * Checks if this node is a number type.
     *
     * @return Returns true if this node is a number type.
     */
    public final boolean isNumberNode() {
        return getType() == NodeType.NUMBER;
    }

    /**
     * Checks if this node is a boolean type.
     *
     * @return Returns true if this node is a boolean type.
     */
    public final boolean isBooleanNode() {
        return getType() == NodeType.BOOLEAN;
    }

    /**
     * Checks if this node is a null type.
     *
     * @return Returns true if this node is a null type.
     */
    public final boolean isNullNode() {
        return getType() == NodeType.NULL;
    }

    /**
     * Gets the node as an ObjectNode if it is an object.
     *
     * @return Returns the optional object node.
     */
    public Optional<ObjectNode> asObjectNode() {
        return Optional.empty();
    }

    /**
     * Gets the node as an ArrayNode if it is an array.
     *
     * @return Returns the optional array node.
     */
    public Optional<ArrayNode> asArrayNode() {
        return Optional.empty();
    }

    /**
     * Gets the node as an StringNode if it is an string.
     *
     * @return Returns the optional StringNode.
     */
    public Optional<StringNode> asStringNode() {
        return Optional.empty();
    }

    /**
     * Gets the node as an BooleanNode if it is an boolean.
     *
     * @return Returns the optional BooleanNode.
     */
    public Optional<BooleanNode> asBooleanNode() {
        return Optional.empty();
    }

    /**
     * Gets the node as an NumberNode if it is an number.
     *
     * @return Returns the optional NumberNode.
     */
    public Optional<NumberNode> asNumberNode() {
        return Optional.empty();
    }

    /**
     * Gets the node as an NullNode if it is a null.
     *
     * @return Returns the optional NullNode.
     */
    public Optional<NullNode> asNullNode() {
        return Optional.empty();
    }

    /**
     * Casts the current node to an {@code ObjectNode}.
     *
     * @return Returns an object node.
     * @throws ExpectationNotMetException when the node is not an {@code ObjectNode}.
     */
    public final ObjectNode expectObjectNode() {
        return expectObjectNode(null);
    }

    /**
     * Casts the current node to an {@code ObjectNode}, throwing
     * {@link ExpectationNotMetException} when the node is the wrong type.
     *
     * @param message Error message to use if the node is of the wrong type.
     * @return Returns an object node.
     * @throws ExpectationNotMetException when the node is not an {@code ObjectNode}.
     */
    public ObjectNode expectObjectNode(String message) {
        throw new ExpectationNotMetException(expandMessage(message, NodeType.OBJECT.toString()), this);
    }

    /**
     * Casts the current node to an {@code ArrayNode}.
     *
     * @return Returns an array node.
     * @throws ExpectationNotMetException when the node is not an {@code ArrayNode}.
     */
    public final ArrayNode expectArrayNode() {
        return expectArrayNode(null);
    }

    /**
     * Casts the current node to an {@code ArrayNode}, throwing
     * {@link ExpectationNotMetException} when the node is the wrong type.
     *
     * @param message Error message to use if the node is of the wrong type.
     * @return Returns an array node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public ArrayNode expectArrayNode(String message) {
        throw new ExpectationNotMetException(expandMessage(message, NodeType.ARRAY.toString()), this);
    }

    /**
     * Casts the current node to a {@code StringNode}.
     *
     * @return Returns a string node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public final StringNode expectStringNode() {
        return expectStringNode(null);
    }

    /**
     * Casts the current node to a {@code StringNode}, throwing
     * {@link ExpectationNotMetException} when the node is the wrong type.
     *
     * @param message Error message to use if the node is of the wrong type.
     * @return Returns a string node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public StringNode expectStringNode(String message) {
        throw new ExpectationNotMetException(expandMessage(message, NodeType.STRING.toString()), this);
    }

    /**
     * Casts the current node to a {@code NumberNode}.
     *
     * @return Returns a number node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public final NumberNode expectNumberNode() {
        return expectNumberNode(null);
    }

    /**
     * Casts the current node to a {@code NumberNode}, throwing
     * {@link ExpectationNotMetException} when the node is the wrong type.
     *
     * @param message Error message to use if the node is of the wrong type.
     * @return Returns a number node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public NumberNode expectNumberNode(String message) {
        throw new ExpectationNotMetException(expandMessage(message, NodeType.NUMBER.toString()), this);
    }

    /**
     * Casts the current node to a {@code BooleanNode}.
     *
     * @return Returns a boolean node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public final BooleanNode expectBooleanNode() {
        return expectBooleanNode(null);
    }

    /**
     * Casts the current node to a {@code BooleanNode}, throwing
     * {@link ExpectationNotMetException} when the node is the wrong type.
     *
     * @param message Error message to use if the node is of the wrong type.
     * @return Returns a boolean node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public BooleanNode expectBooleanNode(String message) {
        throw new ExpectationNotMetException(expandMessage(message, NodeType.BOOLEAN.toString()), this);
    }

    /**
     * Casts the current node to a {@code NullNode}.
     *
     * @return Returns a null node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public final NullNode expectNullNode() {
        return expectNullNode(null);
    }

    /**
     * Casts the current node to a {@code NullNode}, throwing
     * {@link ExpectationNotMetException} when the node is the wrong type.
     *
     * @param message Error message to use if the node is of the wrong type.
     * @return Returns a null node.
     * @throws ExpectationNotMetException when the node is the wrong type.
     */
    public NullNode expectNullNode(String message) {
        throw new ExpectationNotMetException(expandMessage(message, NodeType.NULL.toString()), this);
    }

    @Override
    public final SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public final Node toNode() {
        return this;
    }

    private String expandMessage(String message, String expectedType) {
        return (message == null
                ? format("Expected %s, but found {type}.", expectedType)
                : message).replace("{type}", getType().toString());
    }

    /**
     * Returns a node with sorted keys and sorted keys of all nested object
     * nodes.
     *
     * @return Returns the node in which all object nodes have sorted keys.
     */
    public final Node withDeepSortedKeys() {
        return withDeepSortedKeys(Comparator.comparing(StringNode::getValue));
    }

    /**
     * Returns a node with sorted keys and sorted keys of all nested object
     * nodes using a custom comparator.
     *
     * @param keyComparator Compares keys.
     * @return Returns the node in which all object nodes have sorted keys.
     */
    public final Node withDeepSortedKeys(Comparator<StringNode> keyComparator) {
        return sortNode(this, keyComparator);
    }

    private static Node sortNode(Node node, Comparator<StringNode> keyComparator) {
        return node.accept(new NodeVisitor.Default<Node>() {
            @Override
            protected Node getDefault(Node node) {
                return node;
            }

            @Override
            public Node objectNode(ObjectNode node) {
                Map<StringNode, Node> members = new TreeMap<>(keyComparator);
                node.getMembers().forEach((k, v) -> members.put(k, sortNode(v, keyComparator)));
                return new ObjectNode(members, node.getSourceLocation());
            }

            @Override
            public Node arrayNode(ArrayNode node) {
                return node.getElements().stream()
                        .map(element -> sortNode(element, keyComparator))
                        .collect(ArrayNode.collect(node.getSourceLocation()));
            }
        });
    }
}
