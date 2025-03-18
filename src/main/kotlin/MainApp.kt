import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.scene.layout.Priority
import java.net.URL
import com.google.gson.Gson
import javafx.concurrent.Worker
import javafx.scene.input.KeyCode

// GraphVisualizerApp: A JavaFX application that allows users to visualize and interact with directed graphs.
// The app uses a WebView to display a graph rendered by Cytoscape.js and provides a text input area
// for users to define the graph edges. It also includes a vertex list that lets users enable or disable
// specific vertices (disabled vertices are excluded from the diagram).
class GraphVisualizerApp : Application() {
    // Initial graph definition: Creates a circular graph with 100 nodes.
    private var graphText = buildString {
        for (i in 0 until 100) {
            appendLine("$i -> ${(i + 1) % 100}")
        }
    }

    // Set of vertices that are currently disabled (i.e., should not appear in the graph).
    private var disabledVertices = mutableSetOf<String>()
    // List of vertices parsed from the graph input.
    private var vertices = mutableListOf<String>()
    // Map storing CheckBoxes for each vertex to enable incremental update without recreating the entire list.
    private val vertexCheckBoxes = mutableMapOf<String, CheckBox>()
    private lateinit var webView: WebView
    private lateinit var vertexVBox: VBox

    override fun start(primaryStage: Stage) {
        // Create the main layout container with spacing and padding.
        val root = VBox(10.0).apply { padding = Insets(10.0) }

        // Initialize the WebView for displaying the Cytoscape.js graph.
        webView = WebView().apply {
            prefHeight = 500.0
            minHeight = 500.0
        }
        val webEngine = webView.engine
        webEngine.isJavaScriptEnabled = true

        // Load the HTML file that sets up Cytoscape.js.
        val htmlUrl: URL? = javaClass.classLoader.getResource("graph.html")
        if (htmlUrl == null) {
            println("Error: graph.html not found in resources")
            return
        }
        webEngine.load(htmlUrl.toExternalForm())

        // Listen for the HTML page load to complete.
        webEngine.loadWorker.stateProperty().addListener { _, _, newState ->
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater {
                    // Wait for Cytoscape.js to be ready (with retries).
                    waitForCytoscapeReady(10, 500) { success ->
                        if (success) {
                            updateGraph()
                        } else {
                            println("Error: Cytoscape not ready after retries")
                        }
                    }
                }
            }
        }

        // Add the WebView to the main layout and allow it to grow with the window.
        root.children.add(webView)
        VBox.setVgrow(webView, Priority.ALWAYS)

        // Create a TextArea for graph input with the initial graph definition.
        val graphTextArea = TextArea(graphText).apply {
            prefHeight = 150.0
            maxHeight = 150.0
            isWrapText = true
        }
        val scrollPane = ScrollPane(graphTextArea).apply {
            prefHeight = 150.0
            maxHeight = 150.0
            isFitToWidth = true
        }
        root.children.add(Label("Graph Input Area:"))
        root.children.add(scrollPane)

        // Create a VBox to hold the list of vertices and add it inside a scroll pane.
        val vertexListLabel = Label("Vertex List:")
        vertexVBox = VBox(5.0)
        val vertexScrollPane = ScrollPane(vertexVBox).apply {
            prefHeight = 150.0
            maxHeight = 150.0
            isFitToWidth = true
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        }
        root.children.add(vertexListLabel)
        root.children.add(vertexScrollPane)

        // Add a listener to update the graph and vertex list on any change in the graph input.
        graphTextArea.textProperty().addListener { _, _, newValue ->
            graphText = newValue
            updateVerticesAndGraph(newValue.lines())
        }

        // Also trigger an update when the Enter key is pressed.
        graphTextArea.setOnKeyPressed { event ->
            if (event.code == KeyCode.ENTER) {
                graphText = graphTextArea.text
                updateVerticesAndGraph(graphText.lines())
            }
        }

        // Create and show the scene.
        val scene = Scene(root, 1000.0, 800.0)
        primaryStage.title = "Graph Visualizer with Cytoscape.js"
        primaryStage.scene = scene
        primaryStage.show()

        // Initialize the vertex list from the initial graph text.
        val initialVertices = graphText.lines()
            .filter { it.contains("->") }
            .flatMap { line -> line.split("->").map { it.trim() } }
            .filter { it.isNotEmpty() }
            .distinct()
            .toSet()
        vertices.addAll(initialVertices)
        updateVertexList()
    }

    // Updates both the vertex list and the graph visualization based on the new input lines.
    private fun updateVerticesAndGraph(lines: List<String>) {
        // Parse new vertices from input lines.
        val newVertices = lines
            .filter { it.contains("->") }
            .flatMap { line -> line.split("->").map { it.trim() } }
            .filter { it.isNotEmpty() }
            .distinct()
            .toSet()
        vertices.clear()
        vertices.addAll(newVertices)
        // Update the vertex UI list and the graph diagram.
        updateVertexList()
        updateGraph()
    }

    // Waits until Cytoscape.js is ready in the WebView by repeatedly checking a JS flag.
    // The function will retry for a specified number of times with a delay between checks.
    private fun waitForCytoscapeReady(maxRetries: Int, delayMs: Long, callback: (Boolean) -> Unit) {
        var retries = 0
        fun check() {
            val isReady = webView.engine.executeScript("typeof window.isCytoscapeReady === 'boolean' && window.isCytoscapeReady") as? Boolean
            val isUpdateGraphDefined = webView.engine.executeScript("typeof window.updateGraph === 'function'") as? Boolean
            if (isReady == true && isUpdateGraphDefined == true) {
                callback(true)
            } else if (retries >= maxRetries) {
                callback(false)
            } else {
                retries++
                Thread.sleep(delayMs)
                Platform.runLater { check() }
            }
        }
        Platform.runLater { check() }
    }

    // Incrementally updates the vertex list UI.
    // Instead of recreating all CheckBoxes, it removes only those that no longer exist and adds new ones.
    private fun updateVertexList() {
        val currentVertices = vertices.toSet()

        // Remove CheckBoxes for vertices that have been removed.
        vertexCheckBoxes.keys.filter { it !in currentVertices }.forEach { vertex ->
            val checkBox = vertexCheckBoxes.remove(vertex)
            vertexVBox.children.remove(checkBox)
        }

        // Add CheckBoxes for new vertices.
        currentVertices.forEach { vertex ->
            if (!vertexCheckBoxes.containsKey(vertex)) {
                val checkBox = CheckBox(vertex).apply {
                    // Set selection based on whether the vertex is disabled.
                    isSelected = !disabledVertices.contains(vertex)
                    // Listen for changes to update the disabled set and refresh the graph.
                    selectedProperty().addListener { _, _, newValue ->
                        if (newValue) disabledVertices.remove(vertex) else disabledVertices.add(vertex)
                        updateGraph()
                    }
                }
                vertexCheckBoxes[vertex] = checkBox
                vertexVBox.children.add(checkBox)
            }
        }
    }

    // Updates the graph visualization by computing incremental changes to nodes and edges.
    // It retrieves the current graph elements from the WebView, compares them with new elements
    // derived from the graph input, and updates the Cytoscape diagram accordingly.
    private fun updateGraph() {
        // Parse edges from the graph input.
        val edges = graphText.lines().filter { it.contains("->") }
        val gson = Gson()

        // Retrieve current graph elements (nodes and edges) from the WebView.
        val currentElementsJson = try {
            webView.engine.executeScript("getCurrentElements()") as String
        } catch (e: Exception) {
            "{}"
        }

        // Data class representing the structure of graph elements.
        data class GraphElements(val nodes: List<String>?, val edges: List<String>?)
        val currentElements = gson.fromJson(currentElementsJson, GraphElements::class.java)
        val currentNodes = currentElements.nodes?.toSet() ?: emptySet()
        val currentEdges = currentElements.edges?.toSet() ?: emptySet()

        // Filter new edges by excluding those that include disabled vertices.
        val newEdges = edges.filter { edge ->
            val (source, target) = edge.split("->").map { it.trim() }
            !disabledVertices.contains(source) && !disabledVertices.contains(target)
        }.toSet()
        // Determine new nodes from the filtered edges.
        val newNodes = newEdges.flatMap { edge ->
            edge.split("->").map { it.trim() }
        }.filter { !disabledVertices.contains(it) }.toSet()

        // Determine differences between current and new elements.
        val nodesToAdd = newNodes - currentNodes
        val nodesToRemove = currentNodes - newNodes
        val edgesToAdd = newEdges - currentEdges
        val edgesToRemove = currentEdges - newEdges

        try {
            // Remove nodes and edges that are no longer present.
            if (nodesToRemove.isNotEmpty() || edgesToRemove.isNotEmpty()) {
                val removeScript = """
                    cy.elements().filter(function(element) {
                        if (element.isNode()) {
                            return ${gson.toJson(nodesToRemove)}.includes(element.id());
                        } else if (element.isEdge()) {
                            return ${gson.toJson(edgesToRemove)}.includes(element.data('source') + '->' + element.data('target'));
                        }
                        return false;
                    }).remove();
                """.trimIndent()
                webView.engine.executeScript(removeScript)
            }

            // Prepare new elements to add.
            val elementsToAdd = mutableListOf<Map<String, Map<String, String>>>()
            nodesToAdd.forEach { node ->
                elementsToAdd.add(mapOf("data" to mapOf("id" to node)))
            }
            edgesToAdd.forEach { edge ->
                val (source, target) = edge.split("->").map { it.trim() }
                elementsToAdd.add(mapOf("data" to mapOf("source" to source, "target" to target)))
            }
            if (elementsToAdd.isNotEmpty()) {
                val addScript = "cy.add(${gson.toJson(elementsToAdd)})"
                webView.engine.executeScript(addScript)
            }

            // If there were any changes, re-run the layout to update the visualization.
            if (nodesToAdd.isNotEmpty() || nodesToRemove.isNotEmpty() || edgesToAdd.isNotEmpty() || edgesToRemove.isNotEmpty()) {
                webView.engine.executeScript("""
                    cy.layout({
                        name: 'cose',
                        animate: true,
                        animationDuration: 500,
                        animationEasing: 'ease',
                        randomize: true
                    }).run();
                """.trimIndent())
            }
        } catch (e: Exception) {
            println("Error updating graph: ${e.message}")
        }
    }
}

// Main function to launch the JavaFX application.
fun main() {
    Application.launch(GraphVisualizerApp::class.java)
}
