package com.clawstack.shellguard.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureNoteMarkdownParserTest {

    @Test
    fun `blank content returns empty block list`() {
        assertTrue(parseSecureNoteBlocks("").isEmpty())
        assertTrue(parseSecureNoteBlocks("   \n\t  ").isEmpty())
    }

    @Test
    fun `parses headings bullets paragraphs and blank lines`() {
        val input = """
            # Recovery Keys
            ## Primary
            ### Backup

            - First token
            * Second token
            Plain line with `inline` text
        """.trimIndent()

        val blocks = parseSecureNoteBlocks(input)

        assertEquals(
            listOf(
                SecureNoteBlock.Heading(1, "Recovery Keys"),
                SecureNoteBlock.Heading(2, "Primary"),
                SecureNoteBlock.Heading(3, "Backup"),
                SecureNoteBlock.BlankLine,
                SecureNoteBlock.Bullet("First token"),
                SecureNoteBlock.Bullet("Second token"),
                SecureNoteBlock.Paragraph("Plain line with `inline` text")
            ),
            blocks
        )
    }

    @Test
    fun `parses fenced code block with language and ignores markdown syntax inside`() {
        val input = """
            Intro
            ```bash
            # Not a heading
            - Not a bullet
            echo "hello"
            ```
            Outro
        """.trimIndent()

        val blocks = parseSecureNoteBlocks(input)

        assertEquals(
            listOf(
                SecureNoteBlock.Paragraph("Intro"),
                SecureNoteBlock.CodeBlock(
                    language = "bash",
                    code = "# Not a heading\n- Not a bullet\necho \"hello\""
                ),
                SecureNoteBlock.Paragraph("Outro")
            ),
            blocks
        )
    }

    @Test
    fun `unclosed code fence flushes remaining lines as code block`() {
        val input = "```json\n{\"ok\":true}"
        val blocks = parseSecureNoteBlocks(input)

        assertEquals(
            listOf(
                SecureNoteBlock.CodeBlock(
                    language = "json",
                    code = "{\"ok\":true}"
                )
            ),
            blocks
        )
    }
}
