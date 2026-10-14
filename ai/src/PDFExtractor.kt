package klite.ai

import klite.MimeTypes
import klite.nodes.Node
import klite.toBase64Url
import java.io.InputStream
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Deprecated("Use DataExtractor.extract() with the pdf file as a data: or file: URI, the whole file content is sent to the AI client")
class PDFExtractor(private val extractor: DataExtractor) {
  inline fun <reified T: Any> extractData(pdf: InputStream, provided: Map<KProperty1<*, *>, Any?> = emptyMap(), extraPrompt: String = "", params: Node = emptyMap(), numAttempts: Int = 3): T =
    extractData(pdf, typeOf<T>(), provided, extraPrompt, params, numAttempts)

  fun <T: Any> extractData(pdf: InputStream, type: KType, provided: Map<KProperty1<*, *>, Any?> = emptyMap(), extraPrompt: String = "", params: Node = emptyMap(), numAttempts: Int = 3): T =
    extractor.extract(extraPrompt, type, pdf.use { it.readBytes() }.toBase64Url(MimeTypes.pdf),
      provided = provided, params = params, numAttempts = numAttempts)
}
