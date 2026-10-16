package klite.email

import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.spyk
import io.mockk.verify
import klite.Config
import klite.Email
import klite.MimeTypes
import org.junit.jupiter.api.Test

val email = Email("some@email.com")

class EmailSenderTest {
  init { Config.useEnvFile() }

  @Test fun `instances can be created with default config`() {
    expect(FakeEmailSender()).toBeAnInstanceOf<EmailSender>()
    expect(SmtpEmailSender()).toBeAnInstanceOf<EmailSender>()
  }

  @Test fun translates() {
    val sender = spyk(FakeEmailSender())
    val content = EmailContent("en", "hello", mapOf("name" to "World"))
    sender.send(email, content)
    verify { sender.send(email, content.subject, content.fullHtml(), MimeTypes.html) }
  }

  @Test fun `arbitrary headers are passed to the message`() {
    val sender = FakeEmailSender()
    sender.send(email, EmailContent("en", "hello", headers = mapOf("In-Reply-To" to "<1@example.com>")))
    expect(sender.lastSentEmail).toContain("Headers: {In-Reply-To=<1@example.com>}")
  }
}
