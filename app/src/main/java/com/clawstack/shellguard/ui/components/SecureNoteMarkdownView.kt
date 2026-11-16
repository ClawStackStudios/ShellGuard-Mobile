package com.clawstack.shellguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import com.clawstack.shellguard.ui.theme.TextSecondary

internal sealed interface SecureNoteBlock {
    data class Heading(val level: Int, val text: String) : SecureNoteBlock
    data class Bullet(val text: String) : SecureNoteBlock
    data class CodeBlock(val language: String, val code: String) : SecureNoteBlock
    data class Paragraph(val text: String) : SecureNoteBlock
    data object BlankLine : SecureNoteBlock
}

internal fun parseSecureNoteBlocks(content: String): List<SecureNoteBlock> {
    if (content.isBlank()) return emptyList()

    val blocks = mutableListOf<SecureNoteBlock>()
    val lines = content.replace("\r\n", "\n").split('\n')
    var inCodeBlock = false
    var codeLang = ""
    val codeLines = mutableListOf<String>()

    for (line in lines) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            if (!inCodeBlock) {
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
                codeLines.clear()
            } else {
                blocks.add(SecureNoteBlock.CodeBlock(codeLang, codeLines.joinToString("\n")))
                inCodeBlock = false
                codeLang = ""
                codeLines.clear()
            }
            continue
        }

        if (inCodeBlock) {
            codeLines.add(line)
            continue
        }

        when {
            line.isBlank() -> blocks.add(SecureNoteBlock.BlankLine)
            trimmed.startsWith("### ") -> blocks.add(SecureNoteBlock.Heading(3, trimmed.removePrefix("### ")))
            trimmed.startsWith("## ") -> blocks.add(SecureNoteBlock.Heading(2, trimmed.removePrefix("## ")))
            trimmed.startsWith("# ") -> blocks.add(SecureNoteBlock.Heading(1, trimmed.removePrefix("# ")))
            trimmed.startsWith("- ") -> blocks.add(SecureNoteBlock.Bullet(trimmed.removePrefix("- ")))
            trimmed.startsWith("* ") -> blocks.add(SecureNoteBlock.Bullet(trimmed.removePrefix("* ")))
            else -> blocks.add(SecureNoteBlock.Paragraph(line))
        }
    }

    if (inCodeBlock) {
        blocks.add(SecureNoteBlock.CodeBlock(codeLang, codeLines.joinToString("\n")))
    }

    return blocks
}

private fun formatInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("`", index) -> {
                val close = text.indexOf('`', index + 1)
                if (close != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = BrandClawCyan,
                            background = SurfaceDark
                        )
                    ) {
                        append(text.substring(index + 1, close))
                    }
                    index = close + 1
                } else {
                    append(text[index])
                    index++
                }
            }
            text.startsWith("**", index) -> {
                val close = text.indexOf("**", index + 2)
                if (close != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                        append(text.substring(index + 2, close))
                    }
                    index = close + 2
                } else {
                    append(text[index])
                    index++
                }
            }
            else -> {
                append(text[index])
                index++
            }
        }
    }
}

/**
 * Reef Modernist Markdown renderer for revealed Secure Notes.
 * Supports `#`/`##`/`###` headings, fenced code blocks, `-`/`*` bullets, and inline `` `code` `` / `**bold**`.
 */
@Composable
fun SecureNoteMarkdownView(
    content: String,
    modifier: Modifier = Modifier
) {
    if (content.isBlank()) {
        Text(
            text = "(Empty Note)",
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = modifier
        )
        return
    }

    val blocks = remember(content) { parseSecureNoteBlocks(content) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is SecureNoteBlock.BlankLine -> Spacer(modifier = Modifier.height(6.dp))
                is SecureNoteBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 16.sp
                        else -> 14.sp
                    }
                    val color = if (block.level == 1) ReefPink else TextPrimary
                    Text(
                        text = formatInlineMarkdown(block.text),
                        color = color,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (fontSize.value + 6).sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is SecureNoteBlock.Bullet -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "• ",
                            color = BrandClawCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatInlineMarkdown(block.text),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is SecureNoteBlock.CodeBlock -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OceanDark)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        if (block.language.isNotBlank()) {
                            Text(
                                text = block.language.uppercase(),
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Text(
                                text = block.code,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                is SecureNoteBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.text),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
