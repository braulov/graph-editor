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
    var graphText = buildString {
        for (i in 0 until 100) {
            appendLine("$i -> ${(i + 1) % 100}")
        }
    }

    // Set of vertices that are currently disabled (i.e., should not appear in the graph).
    var disabledVertices = mutableSetOf<String>()
    // List of vertices parsed from the graph input.
    var vertices = mutableListOf<String>()
    // Map storing CheckBoxes for each vertex to enable incremental update without recreating the entire list.
    val vertexCheckBoxes = mutableMapOf<String, CheckBox>()
    lateinit var webView: WebView
    lateinit var vertexVBox: VBox

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
        val initialVertices = parseVertices(graphText.lines())
        vertices.addAll(initialVertices)
        updateVertexList()
    }

    // Updates both the vertex list and the graph visualization based on the new input lines.
    fun updateVerticesAndGraph(lines: List<String>) {
        val newVertices = parseVertices(lines)
        vertices.clear()
        vertices.addAll(newVertices)
        updateVertexList()
        updateGraph()
    }

    // Waits until Cytoscape.js is ready in the WebView by repeatedly checking a JS flag.
    fun waitForCytoscapeReady(maxRetries: Int, delayMs: Long, callback: (Boolean) -> Unit) {
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
    fun updateVertexList() {
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
                    isSelected = !disabledVertices.contains(vertex)
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
    fun updateGraph() {
        val edges = filterEdges(graphText.lines(), disabledVertices)
        val gson = Gson()

        val currentElementsJson = try {
            webView.engine.executeScript("getCurrentElements()") as String
        } catch (e: Exception) {
            "{}"
        }

        data class GraphElements(val nodes: List<String>?, val edges: List<String>?)
        val currentElements = gson.fromJson(currentElementsJson, GraphElements::class.java)
        val currentNodes = currentElements.nodes?.toSet() ?: emptySet()
        val currentEdges = currentElements.edges?.toSet() ?: emptySet()

        val newEdges = edges
        val newNodes = newEdges.flatMap { edge ->
            edge.split("->").map { it.trim() }
        }.toSet()

        val nodesToAdd = newNodes - currentNodes
        val nodesToRemove = currentNodes - newNodes
        val edgesToAdd = newEdges - currentEdges
        val edgesToRemove = currentEdges - newEdges

        try {
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

    // Parses vertices from the input lines.
    fun parseVertices(lines: List<String>): List<String> {
        return lines
            .filter { it.contains("->") }
            .flatMap { line -> line.split("->").map { it.trim() } }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    // Filters edges based on disabled vertices.
    fun filterEdges(lines: List<String>, disabledVertices: Set<String>): Set<String> {
        return lines
            .filter { line -> line.contains("->") }     // игнорируем строки без ->
            .filter { edge ->
                val (source, target) = edge.split("->").map { it.trim() }
                !disabledVertices.contains(source) && !disabledVertices.contains(target)
            }
            .toSet()
    }

}

// Main function to launch the JavaFX application.
fun main() {
    Application.launch(GraphVisualizerApp::class.java)
}