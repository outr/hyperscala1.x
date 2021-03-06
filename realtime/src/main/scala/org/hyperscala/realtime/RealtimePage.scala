package org.hyperscala.realtime

import com.outr.net.communicate.{ConnectionHolder, Connection}
import com.outr.net.http.session.Session
import org.hyperscala.html._
import org.hyperscala.html.attributes.InputType
import org.hyperscala.javascript.JavaScriptContent
import org.hyperscala.javascript.dsl.{window, Statement, JSFunction0}
import org.hyperscala.realtime.event.browser.BrowserError
import org.hyperscala.realtime.event.server._
import org.hyperscala.svg.{Svg, SVGTag}
import org.hyperscala.web.event.server.InvokeJavaScript
import org.hyperscala._
import org.hyperscala.css.{Style, StyleSheetAttribute, StyleSheet}
import org.hyperscala.event.BrowserEvent
import org.hyperscala.web.Webpage
import org.powerscala.hierarchy.ChildLike
import org.powerscala.{LocalStack, Priority}
import org.powerscala.hierarchy.event.{ChildRemovedEvent, ChildAddedEvent, Descendants}
import org.powerscala.json.TypedSupport
import org.powerscala.log.Logging
import org.powerscala.property.Property
import org.powerscala.property.event.PropertyChangeEvent

/**
 * RealtimePage is the management layer for Realtime specific to a webpage instance.
 *
 * @author Matt Hicks <matt@outr.com>
 */
class RealtimePage private(val webpage: Webpage) extends Logging {
  private val _initialized = Property[Boolean](default = Some(false))
  def initialized = _initialized.readOnlyView

  // Fire all BrowserEvents on the specified tag
  webpage.jsonEvent.partial(Unit) {
    case evt: BrowserEvent => if (evt.tag != null) {
      evt.tag.eventReceived.fire(evt)
    } else {
      webpage.body.eventReceived.fire(evt)
    }
    case error: BrowserError => Realtime.errorLogger.get match {
      case Some(f) => f(BrowserErrorEvent(webpage, error))
      case None => // No default handler
    }
  }

  def init() = synchronized {
    if (!initialized()) {
      webpage.head.styleSpaces.removed.on {
        case evt => styleSheetRemoved(evt.value)
      }
      webpage.childAdded.listen(Priority.Normal, Descendants)(childAdded)
      webpage.childRemoved.listen(Priority.Normal, Descendants)(childRemoved)
      webpage.listen[PropertyChangeEvent[_], Unit, Unit]("change", Priority.Normal, Descendants)(propertyChanged)

      _initialized := true
    }
  }

  private def childAdded(evt: ChildAddedEvent[XMLContent]) = {
    val parent = evt.parent.asInstanceOf[IdentifiableTag with Container[IdentifiableTag]]
    evt.child match {
      case child: SVGTag if !child.isInstanceOf[Svg] => {
        val index = parent.contents.indexOf(child)
        val afterId = if (index == 0) null else parent.contents(index - 1).identity
        val parentId = if (index == 0) parent.identity else null
        val svg = child.outputString
        send(InsertSVGContent(svg, afterId, parentId))
      }
      case child: HTMLTag => {
        val index = parent.contents.indexOf(child)
        val append = index == parent.contents.length - 1
        val afterId = if (index == 0 || append) null else parent.contents(index - 1).identity
        val parentId = parent.identity
        val html = child.outputString
        send(InsertHTMLContent(html, afterId, parentId, append))

        if (webpage.rendered()) {                             // Only do this if the page has already rendered
          Markup.rendered(child)                            // Mark child tag as rendered
          child.byTag[HTMLTag].foreach(Markup.rendered)     // Mark all tags children of child tag as rendered
        }
      }
      case js: JavaScriptContent => throw new RuntimeException(s"JS not yet supported! ${js.content}")
    }
  }

  private def childRemoved(evt: ChildRemovedEvent[XMLContent]) = {
    debug(s"childRemoved: $evt")
    evt.child match {
      case t: tag.Text => {
        val parent = t.parent.asInstanceOf[HTMLTag with Container[IdentifiableTag]]
        send(SetHTMLAttribute(parent.identity, "content", parent.outputChildrenString))
      }
      case t: IdentifiableTag => send(RemoveHTMLContent(t.identity))
    }
  }

  private def styleSheetRemoved(styleSheet: StyleSheet) = {
    debug(s"stylesheetRemoved: $styleSheet")
    send(SetSelectorStyle(styleSheet.selectorString, null, null, important = false, styleSheet = true))
  }

  private def propertyChanged(evt: PropertyChangeEvent[_]) = evt.property match {
    case property if RealtimePage.isIgnoring(property, evt.newValue) => // Ignoring this change
    case property: StyleSheetAttribute[_] => ChildLike.parentOf(property) match {
      case styleSheet: StyleSheet => ChildLike.parentOf(styleSheet) match {
        case styleSpaces: StyleSpaces => styleChanged(styleSheet.selectorString, property.style, evt.newValue.asInstanceOf[AnyRef], property.isImportant, styleSheet = true)
        case t: IdentifiableTag => styleChanged(s"#${t.identity}", property.style, evt.newValue.asInstanceOf[AnyRef], property.isImportant, styleSheet = false)
      }
    }
    case property: PropertyAttribute[_] => ChildLike.parentOf(property) match {
      case t: IdentifiableTag => tagPropertyChanged(t, property, evt.oldValue, evt.newValue)
    }
    case _ => // Ignore other properties
  }

  private def styleChanged(selector: String, style: Style[_], value: AnyRef, important: Boolean, styleSheet: Boolean) = {
    debug(s"styleChanged: $selector, $style, $value, $important")
    val anyStyle = style.asInstanceOf[Style[AnyRef]]
    val cssName = style.cssName
    val cssValue = if (value != null) anyStyle.persistence.toString(value, cssName, value.getClass) else null
    send(SetSelectorStyle(selector, cssName, cssValue, important, styleSheet))
  }

  private def tagPropertyChanged(t: IdentifiableTag, property: PropertyAttribute[_], oldValue: Any, newValue: Any) = {
    if (!t.rendered) {
      // Ignore property changes until the item has been rendered
      debug(s"Ignoring tagPropertyChanged (not rendered): ${t.xmlLabel}, attribute: ${property.name} - $oldValue changed to $newValue")
    } else if (property == t.id && oldValue == null) {
      // Ignore initial id change as it happens right before send
    } else if (oldValue == null && newValue == Nil) {
      // Ignore initialization of List property
    } else {
      val excludeCurrentConnection = FormField.changingProperty == property && FormField.changingValue == newValue
      debug(s"tagPropertyChanged: ${t.xmlLabel}.${property.name} - $oldValue changed to $newValue, Exclude current connection? $excludeCurrentConnection, Initted: ${t.rendered}")
      webpage.intercept.renderAttribute.fire(property) match {
        case Some(attribute) => {
          if ((t.isInstanceOf[tag.Text] || t.isInstanceOf[tag.Title]) && property.name == "content") {
            val parent = t.parent.asInstanceOf[HTMLTag with Container[IdentifiableTag]]
            send(SetHTMLAttribute(parent.identity, "content", parent.outputChildrenString), excludeCurrentConnection)
          } else if (t.id == property) {
            send(SetHTMLAttribute(oldValue.asInstanceOf[String], attribute.name, attribute.attributeValue), excludeCurrentConnection)
          } else {
            send(SetHTMLAttribute(t.identity, attribute.name, attribute.attributeValue), excludeCurrentConnection)
          }
        }
        case None => // Attribute change was rejected for render, so we don't send the change
      }
    }
  }

  def send(message: AnyRef, excludeCurrentConnection: Boolean = false) = if (!RealtimePage.dontSend) {     // Make sure we're not ignoring all changes in this thread
    if (excludeCurrentConnection) {
      webpage.broadcastJSON(message, ConnectionHolder.connection)
    } else {
      webpage.broadcastJSON(message)
    }
  }

  def sendRedirect(url: String) = webpage.eval(window.location.href := url)

  def reload(force: Boolean = false) = webpage.eval(window.location.reload(force))

  /**
   * Connects change events for FormField (input, textarea, and select) as well as click events on button and input.
   */
  def connectStandard() = {
    webpage.live[FormField] {
      case field => {
        if (field.changeEvent() == null) field.changeEvent := RealtimeEvent()
        field match {
          case i: tag.Input => if (i.clickEvent() == null) field.clickEvent := RealtimeEvent(preventDefault = false)
          case _ => // Not an input
        }
      }
    }
    webpage.live[tag.Button] {
      case b => if (b.clickEvent() == null) b.clickEvent := RealtimeEvent()
    }
  }

  /**
   * All change and click events fire events to the server and form submits prevent default and send event to server.
   */
  def connectForm() = {
    webpage.live[FormField] {
      case field => {
        if (field.changeEvent() == null) {
          field.changeEvent := RealtimeEvent(preventDefault = false)
        }
        field match {
          case i: tag.Input if i.inputType() == InputType.Button => {
            if (field.clickEvent() == null) {
              field.clickEvent := RealtimeEvent(preventDefault = false)
            }
          }
          case _ => // Not a button input
        }
      }
    }
    webpage.live[tag.Button] {
      case b => if (b.clickEvent() == null) b.clickEvent := RealtimeEvent(preventDefault = false)
    }
    webpage.live[tag.Form] {
      case f => {
        if (f.submitEvent() == null) f.submitEvent := RealtimeEvent()
      }
    }
  }

  /**
   * Sends all form data over realtime upon form submit.
   */
  def connectPost() = {
    webpage.live[tag.Form] {
      case f => if (f.submitEvent() == null) f.submitEvent := RealtimeEvent(fireChange = true)
    }
  }
}

object RealtimePage {
  private val _dontSend = new LocalStack[Boolean]
  private val _ignoringChangeProperty = new ThreadLocal[Property[_]]
  private val _ignoringChangeValue = new ThreadLocal[Any]

  TypedSupport.register("insertHTML", classOf[InsertHTMLContent])
  TypedSupport.register("removeHTML", classOf[RemoveHTMLContent])
  TypedSupport.register("attributeHTML", classOf[SetHTMLAttribute])
  TypedSupport.register("setStyle", classOf[SetSelectorStyle])
  TypedSupport.register("tokenEvent", classOf[TokenBrowserEvent])

  TypedSupport.register("insertSVG", classOf[InsertSVGContent])

  def apply(webpage: Webpage) = webpage.store.getOrSet[RealtimePage]("realtime", new RealtimePage(webpage))

  def dontSend = _dontSend.get().getOrElse(false)

  def dontSend[R](f: => R): R = {
    _dontSend.push(true)
    try {
      f
    } finally {
      _dontSend.pop(true)
    }
  }

  def ignoringChange[T](property: Property[T], value: T) = {
    _ignoringChangeProperty.set(property)
    _ignoringChangeValue.set(value)
    try {
      property := value
    } finally {
      _ignoringChangeProperty.remove()
      _ignoringChangeValue.remove()
    }
  }

  def isIgnoring(property: Property[_], value: Any) = {
    if (_ignoringChangeProperty.get() == property && property() == value) {
      _ignoringChangeProperty.remove()
      _ignoringChangeValue.remove()
      true
    } else {
      false
    }
  }
}

case class TokenBrowserEvent(tag: HTMLTag, token: String) extends BrowserEvent