# klite-email

Provides a way to send plain text or html email over SMTP and read incoming mail over IMAP using *javax.mail*.

Depends on [klite-i18n](../i18n) for translations.

To use it, initialize the correct implementation when creating the Server instance:
```kotlin
register(if (Config.isDev) FakeEmailSender::class else SmtpEmailSender::class)
```

Add approprate content to your translation files, e.g. `en.json`:
```json
{
  "emails": {
    "welcome": {
      "subject": "Welcome to our service",
      "body": "Hello, {name}! Welcome to our service.",
      "action": "Click here to login"
    }
  }
}
```

Then you can send emails like this:
```kotlin
emailSender.send(Email("recipient@hello.ee"), EmailContent("en", "welcome", mapOf("name" to "John"), URI("https://github.com/login")))
```

You can redefine HTML email template by extending `EmailContent` class.

## Reading email over IMAP

Configure the mailbox via `IMAP_HOST`, `IMAP_PORT` (defaults to 993), `IMAP_USER`, `IMAP_PASS` and optional `IMAP_FOLDER` (defaults to `INBOX`).

```kotlin
val reader = ImapEmailReader()

// process new mail, marking each message as seen after the handler succeeds
reader.processUnseen { email ->
  println("${email.from}: ${email.subject}, text=${email.text}, attachments=${email.attachments.keys}")
}

// or just fetch unseen messages without changing flags
val unseen: List<ReceivedEmail> = reader.fetchUnseen()
```

Use `ImapEmailReader.useFolder` for lower-level access (e.g. to move or delete messages).
