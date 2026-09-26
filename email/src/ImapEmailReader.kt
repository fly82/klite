package klite.email

import klite.Config
import klite.debug
import klite.logger
import java.util.*
import javax.mail.*
import javax.mail.Flags.Flag.SEEN
import javax.mail.search.FlagTerm

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

  /** Fetches unseen messages without marking them as read */
  fun fetchUnseen(): List<ReceivedEmail> = useFolder {
    search(FlagTerm(Flags(SEEN), false)).map { it.toReceivedEmail() }
  }

  /** Processes unseen messages one by one, marking each as seen after [handler] returns successfully */
  fun processUnseen(handler: (ReceivedEmail) -> Unit) = useFolder(Folder.READ_WRITE) {
    search(FlagTerm(Flags(SEEN), false)).forEach {
      val email = it.toReceivedEmail()
      handler(email)
      it.setFlag(SEEN, true)
      log.debug("Processed email ${email.id ?: email.subject}")
    }
  }

  /** Opens [folderName] with the given [mode], passing it to [block], then closes everything */
  fun <T> useFolder(mode: Int = Folder.READ_ONLY, block: Folder.() -> T): T {
    val store = session.store
    store.connect()
    return store.use { store ->
      val folder = store.getFolder(folderName)
      folder.open(mode)
      try { folder.block() } finally { folder.close(false) }
    }
  }
}
