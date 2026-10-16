package klite.email

import klite.Email
import klite.MimeTypes
import klite.info
import klite.logger
import javax.mail.Address
import javax.mail.internet.InternetAddress

data class Named(val email: Email, val name: String? = null) {
  override fun toString() = if (name != null) "$name <$email>" else email.toString()
  fun toAddress() = InternetAddress(email.value, name)
}

fun Address.toNamed() = (this as InternetAddress).let { Named(Email(it.address), it.personal) }

interface EmailSender {
  fun send(to: Email, subject: String, body: String, bodyMimeType: String = MimeTypes.text, attachments: Map<String, ByteArray> = emptyMap(),
           cc: List<Email> = emptyList(), bcc: List<Email> = emptyList(), from: Named? = null, headers: Map<String, String> = emptyMap())

  fun send(to: Email, content: EmailContent, attachments: Map<String, ByteArray> = emptyMap(), cc: List<Email> = emptyList(), bcc: List<Email> = emptyList()) =
    send(to, content.subject, content.fullHtml(), MimeTypes.html, attachments, cc, bcc, content.from, content.headers)
}

open class FakeEmailSender: EmailSender {
  private val log = logger()
  lateinit var lastSentEmail: String

  override fun send(to: Email, subject: String, body: String, bodyMimeType: String, attachments: Map<String, ByteArray>, cc: List<Email>, bcc: List<Email>, from: Named?, headers: Map<String, String>) {
    lastSentEmail = """
      Email to $to, CC: $cc, BCC: $bcc
      Subject: $subject
      Body ($bodyMimeType): $body
      ${if (headers.isNotEmpty()) "Headers: $headers" else ""}
      ${if (attachments.isNotEmpty()) "Attachments: ${attachments.keys}" else ""}
    """
    log.info(lastSentEmail)
  }
}
