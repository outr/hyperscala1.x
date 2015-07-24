package org.hyperscala.examples.ui

import org.hyperscala.examples.Example
import org.hyperscala.html._
import org.hyperscala.jquery.Gritter
import org.hyperscala.jquery.ui.Spinner
import org.hyperscala.realtime._
import org.hyperscala.web._

/**
 * @author Matt Hicks <matt@outr.com>
 */
class SpinnerExample extends Webpage with Example {
  require(Realtime)
  require(Gritter)

  body.contents += new tag.P(content = "Example usage of a jQuery UI Spinner.")

  val input = new tag.Input(id = "input", value = "5")
  body.contents += input

  val spinner = Spinner(input)
  spinner.min := 2
  spinner.max := 15
  spinner.value.change.on {
    case evt => valueChanged()
  }

  body.contents += new tag.Button(content = "Set to 8") {
    clickEvent.onRealtime {
      case evt => spinner.value := 8.0
    }
  }

  def valueChanged() = {
    Gritter.add(this.webpage, "Input Value Changed", s"New Value: ${spinner.value()}")
  }
}
