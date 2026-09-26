package klite.email

import java.io.InputStream
import java.util.*
import javax.mail.Address
import javax.mail.Message
import javax.mail.Message.RecipientType
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.InternetAddress

data class ReceivedEmail(
  val id: String?,
  val from: InternetAddress?,
  val to: List<InternetAddress>,
  val cc: List<InternetAddress> = emptyList(),
  val subject: String?,
  val date: Date?,
  val text: String? = null,
  val html: String? = null,
  val attachments: Map<String, ByteArray> = emptyMap(),
)

fun Message.toReceivedEmail(): ReceivedEmail {
  val text = StringBuilder()
  val html = StringBuilder()
  val attachments = mutableMapOf<String, ByteArray>()
  collectBody(text, html, attachments)
  return ReceivedEmail(
    id = getHeader("Message-ID")?.firstOrNull(),
    from = from?.firstOrNull() as? InternetAddress,
    to = recipientsOfType(RecipientType.TO),
    cc = recipientsOfType(RecipientType.CC),
    subject = subject,
    date = sentDate,
    text = text.toString().takeIf { it.isNotEmpty() },
    html = html.toString().takeIf { it.isNotEmpty() },
    attachments = attachments,
  )
}

private fun Message.recipientsOfType(type: RecipientType) = getRecipients(type).addresses

private val Array<Address>?.addresses get() = this?.filterIsInstance<InternetAddress>() ?: emptyList()

private fun Part.collectBody(text: StringBuilder, html: StringBuilder, attachments: MutableMap<String, ByteArray>) {
  when {
    isMimeType("multipart/*") -> {
      val content = content as Multipart
      for (i in 0 until content.count) content.getBodyPart(i).collectBody(text, html, attachments)
    }
    fileName != null || disposition == Part.ATTACHMENT -> attachments[fileName ?: "attachment"] = readBytes()
    isMimeType("text/html") -> html.append(contentAsString())
    isMimeType("text/*") -> text.append(contentAsString())
  }
}

private fun Part.contentAsString() = content.let { if (it is String) it else readBytes().toString(Charsets.UTF_8) }

private fun Part.readBytes(): ByteArray = when (val content = content) {
  is ByteArray -> content
  is String -> content.toByteArray()
  is InputStream -> content.readBytes()
  else -> inputStream.readBytes()
}
