package klite.email

import klite.Config
import klite.info
import klite.logger
import java.util.*
import javax.mail.*
import javax.mail.Flags.Flag.SEEN
import javax.mail.Folder.READ_ONLY
import javax.mail.Folder.READ_WRITE
import javax.mail.search.FlagTerm
import javax.mail.search.SearchTerm

open class ImapEmailReader(
  imapUser: String = Config.required("IMAP_USER"),
  props: Properties = Properties().also {
    it["mail.store.protocol"] = "imaps"
    it["mail.imaps.host"] = Config.optional("IMAP_HOST")
    it["mail.imaps.port"] = Config.optional("IMAP_PORT", "993")
    it["mail.imaps.ssl.protocols"] = "TLSv1.2"
    it["mail.imaps.auth"] = true
  },
  private val authenticator: Authenticator = object: Authenticator() {
    override fun getPasswordAuthentication() = PasswordAuthentication(imapUser, Config.required("IMAP_PASS"))
  },
  private val session: Session = Session.getInstance(props, authenticator),
  private val folderName: String = Config.optional("IMAP_FOLDER", "INBOX"),
) {
  private val log = logger()

  companion object {
    val unseen = FlagTerm(Flags(SEEN), false)
  }

  /** Fetches messages without marking them as read */
  fun fetch(term: SearchTerm = unseen): List<EmailMessage> = useFolder {
    search(term).map { it.toEmailMessage() }
  }

  /** Processes messages one by one, marking each as seen after [handler] returns successfully */
  fun process(term: FlagTerm = unseen, mark: (Message) -> Unit = { it.setFlag(term.flags.systemFlags.first(), true) }, handler: (EmailMessage) -> Unit) = useFolder(READ_WRITE) {
    search(term).forEach {
      val email = it.toEmailMessage()
      handler(email)
      mark(it)
      log.info("Processed email ${email.id ?: email.subject} from ${email.from}")
    }
  }

  /** Opens [folderName] with the given [mode], passing it to [block], then closes everything */
  fun <T> useFolder(mode: Int = READ_ONLY, block: Folder.() -> T): T {
    val store = session.store
    store.connect()
    return store.use { store ->
      store.getFolder(folderName).use { folder ->
        folder.open(mode)
        folder.block()
      }
    }
  }
}
