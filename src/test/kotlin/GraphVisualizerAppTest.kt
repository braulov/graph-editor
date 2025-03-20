import javafx.application.Platform
import javafx.scene.control.Labeled
import javafx.scene.control.TextArea
import javafx.stage.Stage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testfx.framework.junit5.ApplicationTest
import org.testfx.api.FxAssert
import org.testfx.framework.junit5.Start
import org.testfx.matcher.control.LabeledMatchers.hasText
import java.util.concurrent.CountDownLatch

class GraphVisualizerAppTest : ApplicationTest() {

    private lateinit var app: GraphVisualizerApp

    @Start
    override fun start(stage: Stage) {
        app = GraphVisualizerApp()
        app.start(stage) // Запускаем JavaFX-приложение
    }

    @Test
    fun testParsingVerticesFromGraphText() {
        val lines = listOf("a -> b", "b -> c", "invalid line", "c -> a")
        val expectedVertices = setOf("a", "b", "c")

        val vertices = app.parseVertices(lines)
        assertEquals(expectedVertices, vertices.toSet(), "Parsed vertices should match expected set")
    }

    @Test
    fun testFilteringEdgesWithDisabledVertices() {
        app.disabledVertices.add("b")
        val lines = listOf("a -> b", "b -> c", "c -> a")
        val expectedEdges = setOf("c -> a")

        val edges = app.filterEdges(lines, app.disabledVertices)
        assertEquals(expectedEdges, edges, "Edges with disabled vertices should be filtered out")
    }

    @Test
    fun testUpdatingGraphOnInputChange() {
        val initialText = "a -> b\nb -> c\nc -> a"
        val updatedText = "a -> b\nb -> d\nd -> a"

        interact {
            (lookup(".text-area").query() as TextArea).text = initialText
        }
        assertEquals(setOf("a", "b", "c"), app.vertices.toSet())

        interact {
            (lookup(".text-area").query() as TextArea).text = updatedText
        }
        assertEquals(setOf("a", "b", "d"), app.vertices.toSet(), "Vertices should update after input change")
    }

    @Test
    fun testVertexCheckboxesUpdate() {
        val initialText = "a -> b\nb -> c\nc -> a"

        interact {
            (lookup(".text-area").query() as TextArea).text = initialText
        }

        assertNotNull(app.vertexCheckBoxes["a"], "Checkbox for 'a' should exist")
        assertNotNull(app.vertexCheckBoxes["b"], "Checkbox for 'b' should exist")
        assertNotNull(app.vertexCheckBoxes["c"], "Checkbox for 'c' should exist")
    }

    @Test
    fun testWebViewLoaded() {
        interact {
            val state = app.webView.engine.loadWorker.state
            assertNotNull(state, "WebEngine should be initialized")
        }
    }

    @Test
    fun testUpdatingVerticesAndGraph() {
        val lines = listOf("a -> b", "b -> c", "c -> a")
        val latch = CountDownLatch(1)

        Platform.runLater {
            app.updateVerticesAndGraph(lines)
            latch.countDown() // Освобождаем тест после обновления графа
        }

        latch.await() // Ждем завершения обновления графа
        assertEquals(setOf("a", "b", "c"), app.vertices.toSet(), "Vertices should be updated correctly")
    }


    @Test
    fun testUIElementsExist() {
        interact {
            val inputLabel = lookup(".label").queryLabeled()
            val vertexLabel = lookup(".label").nth(1).queryLabeled() // Второй .label

            assertEquals("Graph Input Area:", inputLabel.text)
            assertEquals("Vertex List:", vertexLabel.text)
        }
    }



}
