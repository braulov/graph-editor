package org.example

class Graph {
    val edges = mutableListOf<Pair<String, String>>()
    val vertices = mutableSetOf<String>()
    val disabledVertices = mutableSetOf<String>()

    fun parseInput(input: String) {
        edges.clear()
        vertices.clear()
        input.lines().forEach { line ->
            val parts = line.split("->").map { it.trim() }
            if (parts.size == 2) {
                edges.add(parts[0] to parts[1])
                vertices.add(parts[0])
                vertices.add(parts[1])
            }
        }
    }
}