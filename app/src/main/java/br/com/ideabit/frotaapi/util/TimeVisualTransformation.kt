import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText

class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Limita o tamanho a 4 dígitos (HHMM)
        val trimmed = if (text.text.length >= 4) text.text.take(4) else text.text

        val formatted = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                if (i == 1) append(":") // coloca ':' entre HH e MM
            }
        }

        // Offset mapping para permitir edição correta
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (offset <= 1) offset else offset + 1
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= 2) offset else offset - 1
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
