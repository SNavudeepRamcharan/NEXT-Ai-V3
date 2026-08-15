package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MinimalPrimary

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = parseMarkdownBlocks(markdown)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 16.sp
                        else -> 14.sp
                    }
                    Text(
                        text = buildAnnotatedInlineString(block.text, textColor, MaterialTheme.colorScheme.primary),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(
                        code = block.code,
                        language = block.language
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = (block.indent * 12).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = buildAnnotatedInlineString(block.text, textColor, MaterialTheme.colorScheme.primary),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                lineHeight = 21.sp,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = (block.indent * 12).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = 21.sp,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = buildAnnotatedInlineString(block.text, textColor, MaterialTheme.colorScheme.primary),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                lineHeight = 21.sp,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                is MarkdownBlock.Table -> {
                    MarkdownTableView(block.rows, textColor)
                }

                is MarkdownBlock.Paragraph -> {
                    if (block.text.isNotBlank()) {
                        Text(
                            text = buildAnnotatedInlineString(block.text, textColor, MaterialTheme.colorScheme.primary),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                lineHeight = 21.sp,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableView(rows: List<List<String>>, textColor: Color) {
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        rows.forEachIndexed { index, cols ->
            val isHeader = index == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isHeader) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                cols.forEach { cell ->
                    Text(
                        text = cell.trim(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class BulletItem(val text: String, val indent: Int = 0) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String, val indent: Int = 0) : MarkdownBlock
    data class Table(val rows: List<List<String>>) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = raw.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block starts
        if (line.trimStart().startsWith("```")) {
            val language = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            i++
            continue
        }

        // Headers
        val trimmed = line.trim()
        if (trimmed.startsWith("### ")) {
            blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ")))
            i++
            continue
        } else if (trimmed.startsWith("## ")) {
            blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ")))
            i++
            continue
        } else if (trimmed.startsWith("# ")) {
            blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ")))
            i++
            continue
        }

        // Table detection
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            val tableRows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                val tLine = lines[i].trim()
                // Skip separator rows like |---|---|
                if (!tLine.matches(Regex("""\|[\s\-:|]+\|"""))) {
                    val cells = tLine.removeSurrounding("|", "|").split("|").map { it.trim() }
                    tableRows.add(cells)
                }
                i++
            }
            if (tableRows.isNotEmpty()) {
                blocks.add(MarkdownBlock.Table(tableRows))
            }
            continue
        }

        // Bullet lists
        if (trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            val bulletText = trimmed.substring(2)
            val indent = (line.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)) / 2
            blocks.add(MarkdownBlock.BulletItem(bulletText, indent))
            i++
            continue
        }

        // Numbered list
        val numberMatch = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
        if (numberMatch != null) {
            val num = numberMatch.groupValues[1]
            val content = numberMatch.groupValues[2]
            val indent = (line.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)) / 2
            blocks.add(MarkdownBlock.NumberedItem(num, content, indent))
            i++
            continue
        }

        // Regular paragraph
        if (trimmed.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
        i++
    }

    return blocks
}

private fun buildAnnotatedInlineString(raw: String, baseColor: Color, primaryColor: Color) = buildAnnotatedString {
    val inlineCodeRegex = Regex("`([^`]+)`")
    val boldRegex = Regex("\\*\\*([^*]+)\\*\\*")
    val italicRegex = Regex("\\*([^*]+)\\*")

    var idx = 0
    while (idx < raw.length) {
        if (raw.startsWith("**", idx)) {
            val end = raw.indexOf("**", idx + 2)
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                    append(raw.substring(idx + 2, end))
                }
                idx = end + 2
                continue
            }
        } else if (raw.startsWith("`", idx)) {
            val end = raw.indexOf("`", idx + 1)
            if (end != -1) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = primaryColor.copy(alpha = 0.15f),
                        color = baseColor,
                        fontSize = 13.sp
                    )
                ) {
                    append(" ${raw.substring(idx + 1, end)} ")
                }
                idx = end + 1
                continue
            }
        } else if (raw.startsWith("*", idx) && !raw.startsWith("**", idx)) {
            val end = raw.indexOf("*", idx + 1)
            if (end != -1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                    append(raw.substring(idx + 1, end))
                }
                idx = end + 1
                continue
            }
        }
        append(raw[idx])
        idx++
    }
}

