package org.hyperscala.examples.ui

import org.hyperscala.css.attributes._
import org.hyperscala.examples.Example
import org.hyperscala.html._
import org.hyperscala.jquery.Gritter
import org.hyperscala.realtime.RealtimeEvent
import org.hyperscala.ui.widgets.{MultiSelect, Selectable}
import org.hyperscala.web._

import scala.language.reflectiveCalls

/**
 * @author Matt Hicks <matt@outr.com>
 */
class MultiSelectExample extends Webpage with Example {
  require(Gritter)

  val select = new MultiSelect[Int] {
    style.height := 100.px
    style.overflow := Overflow.Auto

    val availableValues = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    def value(t: Int) = t.toString

    def label(t: Int) = t match {
      case 1 => "One"
      case 2 => "Two"
      case 3 => "Three"
      case 4 => "Four"
      case 5 => "Five"
      case 6 => "Six"
      case 7 => "Seven"
      case 8 => "Eight"
      case 9 => "Nine"
      case 10 => "Ten"
    }

    override def createEntry(t: Int, checked: Boolean): Selectable[Int] = {
      val e = super.createEntry(t, checked)
      e.style.display := Display.Block
      e
    }
  }
  body.contents += select

  select.selected := List(3, 4)

  body.contents += new tag.Button(content = "Select 1 and 5") {
    clickEvent := RealtimeEvent()

    clickEvent.on {
      case evt => select.selected := List(1, 5)
    }
  }

  body.contents += new tag.Button(content = "Output Selected") {
    clickEvent := RealtimeEvent()

    clickEvent.on {
      case evt => Gritter.add(this.webpage, "Selected Items", select.selected().mkString(", "))
    }
  }
}
