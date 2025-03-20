
# Graph Visualizer

Graph Visualizer is a GUI application built with Kotlin and JavaFX, designed for visualizing and interacting with directed graphs. It allows users to define graphs via a simple text input and dynamically toggle the visibility of vertices.

## Installation

1.  Clone the repository:
    
    ```sh
    git clone https://github.com/brauov/graph-visualizer.git
    ```
2.  Navigate to the project directory:
    
    ```sh
    cd graph-visualize
    ```
3.  Build the project using Gradle:
    
    ```sh
    ./gradlew build
    ```
## Usage

Run the application:

```sh
./gradlew run
```

Enter the list of edges in the "Graph Input Area", for example:

```text
A -> B
B -> C
C -> A
```

**Note:** For better performance, it is recommended to add edges in full (e.g., by copying and pasting the text) rather than typing them character by character. This is because each text change triggers a graph update, which can slow down the interface for large graphs.

-   Use the checkboxes in the "Vertex List" to enable or disable specific vertices.

## Testing

The project includes a set of unit tests to verify the core logic of the application:

-   **Vertex Parsing:** Ensures that vertices are correctly extracted from the text input.
-   **Edge Filtering:** Verifies that edges connected to disabled vertices are not displayed.
-   **UI Updates:** Checks that the interface updates correctly when the input or vertex states change.

To run the tests, execute:

```bash
./gradlew test
```

The tests are written using JUnit 5 and TestFX for JavaFX component testing.

## Why Cytoscape.js?

I chose Cytoscape.js (often referred to as "cy") for graph visualization because it is a well-known and widely-used library that relies on force-directed layout algorithms. These algorithms enable efficient rendering and smooth updates to the graph’s appearance whenever edges are added or removed. This makes Cytoscape.js an excellent fit for dynamic graphs where the structure changes frequently, ensuring both performance and a good user experience.

## Dependencies

-   Kotlin 1.9.10
-   JavaFX 21
-   Gson 2.10.1
-   Cytoscape.js (embedded in resources)
-   TestFX (for UI testing)
