import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object VisualTransformationUtil {

    fun dataMask(): VisualTransformation = VisualTransformation { text ->

        // texto "12345678" vira "12-34-5678"
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text

        val builder = StringBuilder()
        for (i in trimmed.indices) {
            builder.append(trimmed[i])
            if (i == 1 || i == 3) builder.append('-')
        }

        val output = builder.toString()

        // Mapeia a posição do cursor do original para o transformado
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return output.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return trimmed.length
            }
        }

        TransformedText(AnnotatedString(output), offsetMapping)
    }

    fun horaMask(): VisualTransformation = VisualTransformation { text ->
        // texto "1234" vira "12:34"
        val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text

        val builder = StringBuilder()
        for (i in trimmed.indices) {
            builder.append(trimmed[i])
            if (i == 1) builder.append(':')
        }

        val output = builder.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return output.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return trimmed.length
            }
        }

        TransformedText(AnnotatedString(output), offsetMapping)
    }
}
