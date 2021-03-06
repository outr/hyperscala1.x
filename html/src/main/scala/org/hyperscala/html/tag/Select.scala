package org.hyperscala.html.tag

import org.hyperscala._
import css.StyleSheet
import html.{FormField, HTMLTag}
import org.hyperscala.html.attributes._
import org.hyperscala.html.constraints._
import org.hyperscala.persistence.ValuePersistence
import org.powerscala.property.{PropertyView, ListProperty, Property}
import java.util.concurrent.atomic.AtomicBoolean

/**
 * NOTE: This file has been generated. Do not modify directly!
 * @author Matt Hicks <matt@outr.com>
 */
class Select extends Container[Option] with BodyChild with HTMLTag with FormField {
  implicit val thisSelect = this
  override def xmlExpanded = true

  lazy val xmlLabel = "select"

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
           autoFocus: java.lang.Boolean = null,
           disabled: java.lang.Boolean = null,
           form: String = null,
           multiple: java.lang.Boolean = null,
           size: java.lang.Integer = null,
           placeHolder: String = null,
           content: Option = null) = {
    this()
    init(name, accessKey, clazz, contentEditable, contextMenu, dir, draggable, dropZone, hidden, id, lang, role, spellCheck, style, tabIndex, titleText)
    up(this.autoFocus, autoFocus)
    up(this.disabled, disabled)
    up(this.form, form)
    up(this.multiple, multiple)
    up(this.size, size)
    up(this.placeHolder, placeHolder)
    if (content != null) contents += content
  }

  lazy val autoFocus = PropertyAttribute[Boolean]("autofocus", false)
  lazy val disabled = PropertyAttribute[Boolean]("disabled", false)
  lazy val form = PropertyAttribute[String]("form", null)
  lazy val multiple = PropertyAttribute[Boolean]("multiple", false)
  lazy val size = PropertyAttribute[Int]("size", 1)
  lazy val placeHolder = PropertyAttribute[String]("placeholder", null)

  implicit def optionsPersistence: ValuePersistence[List[Option]] = new ValuePersistence[List[Option]] {
    override def fromString(s: String, name: String, clazz: Class[_]): List[Option] = s.split("|").map(optionByValue).flatten.toList

    override def toString(t: List[Option], name: String, clazz: Class[_]) = t.map(_.value()).mkString("|")
  }

  val selectedOptions = new Property[List[Option]](default = Some(Nil)) with ListProperty[Option]
  val selected = new Property[List[String]](default = Some(Nil)) with ListProperty[String]
  val value = new Property[String]()

  private val updating = new AtomicBoolean(false)
  private def tryUpdating(f: => Unit) = if (updating.compareAndSet(false, true)) {
    try {
      f
    } finally {
      updating.set(false)
    }
  }

  def optionByValue(value: String) = contents.find(o => o.value() == value)

  childAdded.on {
    case evt => evt.child match {
      case o: Option => updateSelectionFromOptions()
    }
  }
  childRemoved.on {
    case evt => evt.child match {
      case o: Option => updateSelectionFromOptions()
    }
  }
  selectedOptions.change.on {
    case evt => updateOptionsFromSelection()              // Update the option tags' selected state upon change
  }
  selected.change.on {
    case evt => selectedOptions := evt.newValue.map(s => optionByValue(s)).flatten
  }
  value.change.on {
    case evt => evt.newValue match {
      case null => selected := Nil
      case s => selected := List(s)
    }
  }

  private[html] def updateSelectionFromOptions() = tryUpdating {
    val options = contents.collect {
      case o if o.selected() => o
    }.toList
    selectedOptions := options
    selected := options.map(o => o.value())
    value := selected().mkString(", ")
  }

  private def updateOptionsFromSelection() = tryUpdating {
    val selected = selectedOptions()
    contents.foreach {
      case o => o.selected := selected.contains(o)
    }
  }

  override def formValue = value
}