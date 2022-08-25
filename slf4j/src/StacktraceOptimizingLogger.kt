package klite.slf4j

/**
 * Use this logger if you want shorter stack traces:
 * Config["LOGGER_CLASS"] = StacktraceOptimizingLogger::class
 */
open class StacktraceOptimizingLogger(name: String): KliteLogger(name) {
  public override fun print(formatted: String, t: Throwable?) {
    out.println(formatted)
    if (t != null) {
      val stackTrace = t.stackTrace
      out.println(t.toString())
      for (i in 0..findUsefulStackTraceEnd(stackTrace)) {
        out.print("  at ")
        out.println(stackTrace[i].toString())
      }
    }
  }

  protected fun findUsefulStackTraceEnd(it: Array<out StackTraceElement>): Int {
    var until = 0
    for (i in it.lastIndex downTo 0) {
      if (it[i].className.run { startsWith("klite") || until > 0 && startsWith("kotlinx.coroutines") }) until = i
      else if (until > 0) break
    }
    return until
  }
}

