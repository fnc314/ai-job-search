package dev.fnc314.aijobtracker.internal.terminal.roadmap

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoadmapAppTest {

    @Test
    fun testRoadmapParserLoadsRoadmapMd() {
        val rootDir = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "ROADMAP.md").exists() }
        val roadmapFile = File(rootDir, "ROADMAP.md")
        assertTrue(roadmapFile.exists(), "ROADMAP.md should exist in project root")

        val items = RoadmapParser.parse(roadmapFile)
        assertTrue(items.isNotEmpty(), "RoadmapParser should parse items from ROADMAP.md")

        val rm010 = items.find { it.id == "RM-010" }
        assertNotNull(rm010, "RM-010 should be parsed")
    }
}
