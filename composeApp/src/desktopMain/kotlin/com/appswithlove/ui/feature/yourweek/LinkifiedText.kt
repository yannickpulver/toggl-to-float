package com.appswithlove.ui.feature.yourweek

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.appswithlove.ui.utils.openInBrowser
import java.net.URI

@Composable
fun LinkifiedText(text: String) {
    val annotatedString = createAnnotatedStringWithLinks(text)

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    println("Clicked on link: ${annotation.item}")
                    openInBrowser(URI(annotation.item))
                    // Here you handle the link click, for example, by opening it in a browser.
                }
        },
    )
}

fun createAnnotatedStringWithLinks(input: String): AnnotatedString {
    val regex = """https?://[^\s]+""".toRegex()
    val builder = AnnotatedString.Builder()

    var lastMatchEnd = 0

    regex.findAll(input).forEach { matchResult ->
        val start = matchResult.range.first
        val end = matchResult.range.last + 1

        // Append text leading up to the match
        builder.append(input.substring(lastMatchEnd, start))

        // Append the matched URL, underline it, and annotate it
        with(builder) {
            pushStringAnnotation("URL", matchResult.value)
            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(matchResult.value)
            }
            pop()
        }

        lastMatchEnd = end
    }

    // Append any remaining text after the last match
    builder.append(input.substring(lastMatchEnd))

    return builder.toAnnotatedString()
}
