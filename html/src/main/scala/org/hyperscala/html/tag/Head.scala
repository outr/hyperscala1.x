package org.hyperscala.html.tag

import org.hyperscala._
import org.hyperscala.css.StyleSheet
import org.hyperscala.html.{StyleSpaces, HTMLTag}
import javascript.JavaScriptContent
import org.hyperscala.html.attributes._
import org.hyperscala.html.constraints._
import scala.Some
import org.hyperscala.selector.Selector

/**
 * NOTE: This file has been generated. Do not modify directly!
 * @author Matt Hicks <matt@outr.com>
 */
class Head extends Container[HeadChild] with HTMLChild with HTMLTag {
  lazy val xmlLabel = "head"
  override def xmlExpanded = true

  private var temporalScripts = List.empty[Script]

  def this(name: String = null,
           accessKey: java.lang.Character = null,
           clazz: Seq[String] = null,
           contentEditable: ContentEditable = null,
           contextMenu: String = null,
           dir: Direction = null,
           draggable: Draggable = null,
           dropZone: DropZone = null,
           hidden: java.lang.Boolean = null,
           id: String = null,
           lang: String = null,
           role: String = null,
           spellCheck: java.lang.Boolean = null,
           style: StyleSheet = null,
           tabIndex: java.lang.Integer = null,
           titleText: String = null,
           content: HeadChild = null) = {
    this()
    init(name, accessKey, clazz, contentEditable, contextMenu, dir, draggable, dropZone, hidden, id, lang, role, spellCheck, style, tabIndex, titleText)
    if (content != null) contents += content
  }

  def title = byTag[Title].headOption match {
    case Some(t) => t.content
    case None => {
      val t = new Title
      contents += t
      t.content
    }
  }

  override protected def generateChildFromTagName(name: String) = {
    if (name == "title") {
      contents.find(c => c.xmlLabel == name) match {
        case Some(tag) => tag
        case None => super.generateChildFromTagName(name)
      }
    } else {
      super.generateChildFromTagName(name)
    }
  }

  override protected def before() {
    super.before()

    // Remove temporal scripts
    synchronized {
      temporalScripts.foreach {
        case script => contents -= script
      }
      temporalScripts = Nil
    }
  }

  def styleSpaces = synchronized {
    byTag[StyleSpaces].headOption match {
      case Some(styleSpaces) => styleSpaces
      case None => {
        val styleSpaces = new StyleSpaces(this)
        contents += styleSpaces
        styleSpaces
      }
    }
  }

  /**
   * Gets or creates a StyleSheet for the selector provided. If a StyleSheet already exists for the supplied selector
   * it will be returned instead of creating a new StyleSheet.
   *
   * @param selector for the requested StyleSheet
   * @return StyleSheet
   */
  def selector(selector: Selector) = synchronized {
    val selectorString = selector.value
    styleSpaces.get(selectorString) match {
      case Some(styleSheet) => styleSheet
      case None => {
        val styleSheet = new StyleSheet(styleSpaces, selector)
        styleSpaces(selectorString) = styleSheet
        styleSheet
      }
    }
  }

  def useStyle[R](selector: Selector)(f: StyleSheet => R) = {
    val style = this.selector(selector)
    f(style)
  }

  def deleteSelector(selector: Selector) = synchronized {
    val selectorString = selector.value
    styleSpaces.remove(selectorString)
  }

  def injectScript(content: JavaScriptContent, temporal: Boolean = false) = {
    val script = new Script {
      contents += content
    }
    if (temporal) {
      synchronized {
        temporalScripts = script :: temporalScripts
      }
    }
    contents += script
  }

  def verifyScript(url: String) = byType[Script].find(s => url.equalsIgnoreCase(s.src())) match {
    case Some(s) => // Already included
    case None => contents += new Script(src = url)
  }

  def verifyCSS(url: String) = byType[Link].find(l => url.equalsIgnoreCase(l.href())) match {
    case Some(l) => // Already included
    case None => contents += new Link(href = url)
  }

  def meta(name: String): scala.Option[String] =
    contents.collectFirst {
      case m: Meta if m.name().toLowerCase == name => m.content()
    }

  def meta(name: String, content: String) = {
    contents.collectFirst {
      case m: Meta if m.name() == name => m
    } match {
      case Some(m) => m.content := content
      case None => contents += new Meta(name = name, content = content)
    }
  }

  def charset(charset: String = "UTF-8") = {
    contents.collectFirst {
      case m: Meta if m.charset() != null && m.charset().nonEmpty => m
    } match {
      case Some(m) => m.charset := charset
      case None => contents += new Meta(charset = charset)
    }
  }
}