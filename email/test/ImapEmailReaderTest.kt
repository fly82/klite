package klite.email

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toContainExactly
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import klite.Config
import klite.Email
import klite.MimeTypes
import org.junit.jupiter.api.Test
import java.util.*
import javax.mail.*
import javax.mail.Message.RecipientType.CC
import javax.mail.Message.RecipientType.TO
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class ImapEmailReaderTest {
  init { Config.useEnvFile() }
  val session = mockk<Session>(relaxed = true)
  val store = mockk<Store>(relaxed = true)
  val folder = mockk<Folder>(relaxed = true)
  val reader = ImapEmailReader(session = session)

  init {
    every { session.store } returns store
    every { store.getFolder(any<String>()) } returns folder
    every { folder.search(any()) } returns emptyArray()
  }

  @Test fun `can be created with default config`() {
    expect(ImapEmailReader(session = session)).toBeAnInstanceOf<ImapEmailReader>()
  }

  @Test fun `parses plain text email`() {
    val email = message {
      setFrom(InternetAddress("from@example.com", "Sender"))
      setRecipient(TO, InternetAddress("to@example.com"))
      subject = "Hello"
      setText("Hi there")
    }.toEmailMessage()

    expect(email.subject).toEqual("Hello")
    expect(email.from).toEqual(Named(Email("from@example.com"), "Sender"))
    expect(email.to).toContainExactly(Named(Email("to@example.com"), null))
    expect(email.text).toEqual("Hi there")
    expect(email.html).toEqual(null)
    expect(email.attachments).toEqual(emptyMap())
  }

  @Test fun `parses multipart with html and attachment`() {
    val email = message {
      setFrom(InternetAddress("from@example.com"))
      subject = "Files"
      setContent(MimeMultipart().apply {
        addBodyPart(MimeBodyPart().apply {
          setContent("<p>Hello</p>", MimeTypes.withCharset(MimeTypes.html))
        })
        addBodyPart(MimeBodyPart().apply {
          setContent("PDF content".toByteArray(), "application/pdf")
          fileName = "hello.pdf"
          disposition = Part.ATTACHMENT
        })
      })
    }.toEmailMessage()

    expect(email.subject).toEqual("Files")
    expect(email.html).toEqual("<p>Hello</p>")
    expect(email.text).toEqual(null)
    expect(email.attachments.keys).toContainExactly("hello.pdf")
    expect(email.attachments["hello.pdf"]!!.decodeToString()).toEqual("PDF content")
  }

  @Test fun `parses cc recipients and message id`() {
    val email = message {
      setFrom(InternetAddress("from@example.com"))
      setRecipient(TO, InternetAddress("to@example.com"))
      setRecipient(CC, InternetAddress("cc@example.com"))
      subject = "Cc"
      setText("Body")
    }.apply { setHeader("Message-ID", "<id@example.com>") }.toEmailMessage()

    expect(email.cc.map { it.email }).toContainExactly(Email("cc@example.com"))
    expect(email.id).toEqual("<id@example.com>")
  }

  @Test fun `fetchUnseen does not mark messages as read`() {
    val message = spyk(message())
    every { folder.search(any()) } returns arrayOf(message)
    val emails = reader.fetchUnseen()
    expect(emails.size).toEqual(1)
    expect(emails.first().subject).toEqual("Subject")
    verify(exactly = 0) { message.setFlag(Flags.Flag.SEEN, true) }
  }

  @Test fun `processUnseen marks messages as read after handler`() {
    val message = spyk(message())
    every { folder.search(any()) } returns arrayOf(message)
    val processed = mutableListOf<String?>()
    reader.processUnseen { processed += it.subject }
    expect(processed).toContainExactly("Subject")
    verify { message.setFlag(Flags.Flag.SEEN, true) }
  }

  @Test fun `processUnseen keeps unread if handler fails`() {
    val message = spyk(message())
    every { folder.search(any()) } returns arrayOf(message)
    expect(runCatching { reader.processUnseen { error("boom") } }.isFailure).toEqual(true)
    verify(exactly = 0) { message.setFlag(Flags.Flag.SEEN, true) }
  }

  private fun message(block: MimeMessage.() -> Unit = {}) = MimeMessage(Session.getInstance(Properties())).apply {
    setFrom(InternetAddress("from@example.com"))
    subject = "Subject"
    setText("Body")
    block()
    saveChanges()
  }
}
