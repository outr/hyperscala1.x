package org.hyperscala.ui.widgets

import org.hyperscala.module.Module
import org.powerscala.Version
import org.hyperscala.web._
import org.hyperscala.html._
import org.hyperscala.html.constraints.BodyChild
import org.hyperscala.html.attributes.InputType
import org.powerscala.Unique
import com.outr.net.http.session.Session

/**
 * @author Matt Hicks <matt@outr.com>
 */
class CSSTree extends tag.Div(clazz = List("css-treeview")) {
  this.require(CSSTree)

  val list = new tag.Ul
  contents += list

  def +=(element: CSSElement) = list.contents += element
}

trait CSSElement extends tag.Li

class CSSBranch(display: BodyChild) extends CSSElement {
  val input = new tag.Input(inputType = InputType.CheckBox, id = Unique())
  val label = new tag.Label(forElement = input.id(), content = display)
  val container = new tag.Ul

  contents += input
  contents += label
  contents += container

  def +=(element: CSSElement) = container.contents += element
}

class CSSLeaf(display: BodyChild) extends CSSElement {
  clazz += "leaf"

  contents += display
}

object CSSTree extends Module {
  val name = "csstree"
  val version = Version(1)

  override def init(website: Website) = {
    website.addClassPath("/csstree/", "csstree/")
  }

  override def load(webpage: Webpage) = {
    webpage.head.contents += new tag.Link(href = "/csstree/style.css", rel = "stylesheet")
  }
}