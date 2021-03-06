package org.hyperscala.ui.widgets

import org.hyperscala.module.Module
import org.powerscala.concurrent.AtomicBoolean
import org.powerscala.event.Listenable
import org.powerscala.property.Property
import org.powerscala.{StorageComponent, Version}
import org.hyperscala.jquery.{jQueryComponent, JavaScriptCaller}
import org.hyperscala.web._
import org.hyperscala.html._
import org.hyperscala.realtime.Realtime
import org.hyperscala.javascript.{JavaScriptString, JavaScriptContent}
import com.outr.net.http.session.Session
import scala.language.implicitConversions

/**
 * Select2 module is a light-weight jQuery wrapper around:
 *
 * http://ivaynberg.github.io/select2
 *
 * @author Matt Hicks <matt@outr.com>
 */
object Select2 extends Module with JavaScriptCaller with StorageComponent[Select2, tag.Select] {
  val name = "select2"
  val version = Version(3, 5, 0)
  var debug = false

  implicit def tag2Select(t: tag.Select): Select2 = apply(t)

  override def apply(t: tag.Select) = {
    t.require(this)
    super.apply(t)
  }

  protected def create(t: tag.Select) = new Select2(t)

  lazy val DontEscapeMarkup = JavaScriptString("function(m) { return m; }")

  override def dependencies = List(Realtime)

  override def init(website: Website) = {
    website.addClassPath("/select2/", "select2-3.5.0/")
  }

  override def load(page: Webpage) = {
    page.head.contents += new tag.Link(href = "/select2/select2.css", rel = "stylesheet")
    if (debug) {
      page.head.contents += new tag.Script(mimeType = "text/javascript", src = "/select2/select2.js")
    } else {
      page.head.contents += new tag.Script(mimeType = "text/javascript", src = "/select2/select2.min.js")
    }

  }
}

class Select2(val wrapped: tag.Select, val autoInit: Boolean = true) extends jQueryComponent {
  def functionName = "select2"

  implicit def listenable: Listenable = wrapped

  val width = property("width", "")
  val minimumInputLength = property("minimumInputLength", -1)
  val maximumInputLength = property("maximumInputLength", -1)
  val minimumResultsForSearch = property("minimumResultsForSearch", -1)
  val maximumSelectionSize = property("maximumSelectionSize", -1)
  val placeholder = property("placeholder", "")
  val placeholderOption = property("placeholderOption", "")
  val separator = property("separator", ",")
  val allowClear = property("allowClear", true)
  val multiple = wrapped.multiple
  val closeOnSelect = property("closeOnSelect", true)
  val openOnEnter = property("openOnEnter", true)
  val matcher = property[JavaScriptContent]("matcher", null)
  val sortResults = property[JavaScriptContent]("sortResults", null)
  val formatSelection = property[JavaScriptContent]("formatSelection", null)
  val formatResult = property[JavaScriptContent]("formatResult", null)
  val formatResultCssClass = property[JavaScriptContent]("formatResultCssClass", null)
  val formatNoMatches = property[JavaScriptContent]("formatNoMatches", null)
  val formatSearching = property[JavaScriptContent]("formatSearching", null)
  val formatInputTooShort = property[JavaScriptContent]("formatInputTooShort", null)
  val formatInputTooLong = property[JavaScriptContent]("formatInputTooLong", null)
  val formatSelectionTooBig = property[JavaScriptContent]("formatSelectionTooBig", null)
  val formatLoadMore = property[JavaScriptContent]("formatLoadMore", null)
  val createSearchChoice = property[JavaScriptContent]("createSearchChoice", null)
  val createSearchChoicePosition = property[JavaScriptContent]("createSearchChoicePosition", null)
  val initSelection = property[JavaScriptContent]("initSelection", null)
  val tokenizer = property[JavaScriptCaller]("tokenizer", null)
  val tokenSeparators = property("tokenSeparators", List.empty[String])
  val query = property[JavaScriptContent]("query", null)
  val tags = property[JavaScriptContent]("tags", null)
  val containerCss = property[JavaScriptContent]("containerCss", null)
  val containerCssClass = property[String]("containerCssClass", null)
  val dropdownCss = property[JavaScriptContent]("dropdownCss", null)
  val dropdownCssClass = property[String]("dropdownCssClass", null)
  val dropdownAutoWidth = property("dropdownAutoWidth", false)
  val adaptContainerCssClass = property[JavaScriptContent]("adaptContainerCssClass", null)
  val adaptDropdownCssClass = property[JavaScriptContent]("adaptDropdownCssClass", null)
  val escapeMarkup = property[JavaScriptContent]("escapeMarkup", null)
  val selectOnBlur = property("selectOnBlur", false)
  val loadMorePadding = property("loadMorePadding", 0)
  val nextSearchTerm = property[JavaScriptContent]("nextSearchTerm", null)
  val value = Property[String](default = Option(wrapped.value()))
  val values = Property[List[String]](default = Option(wrapped.selected()))

  private val changing = new AtomicBoolean
  private def doChange(f: => Unit) = if (changing.compareAndSet(false, true)) {
    try {
      f
    } finally {
      changing.set(false)
    }
  }
  value.change.on {
    case evt => doChange {
      values := List(evt.newValue)

      afterInit {
        call("val", JavaScriptString(s"${JavaScriptContent.toJS(evt.newValue)}, true"))
      }
    }
  }
  values.change.on {
    case evt => doChange {
      value := evt.newValue.mkString(", ")

      afterInit {
        call("val", JavaScriptString(s"${JavaScriptContent.toJS(evt.newValue)}, true"))
      }
    }
  }

  wrapped.changeEvent.on {
    case evt => doChange {
      val s = wrapped.value()
      value := s
      values := (if (s != null) s.split("[|]").toList else Nil)
    }
  }
}