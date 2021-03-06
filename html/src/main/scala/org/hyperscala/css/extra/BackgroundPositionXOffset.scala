package org.hyperscala.css.extra

import org.powerscala.property.{DerivedProperty, Property}
import org.hyperscala.css.attributes.{BackgroundPosition, Length, BackgroundRepeat, Repeat}
import org.hyperscala.css.StyleSheet
import org.powerscala.event.Listenable

/**
 * @author Matt Hicks <matt@outr.com>
 */
class BackgroundPositionXOffset(ss: StyleSheet) extends Property[Length]()(ss, implicitly[Manifest[Length]]) with DerivedProperty[Length, BackgroundPosition] {
  implicit def styleSheetListenable: Listenable = ss

  def other = ss.backgroundPosition

  def fromT(value: Length) = {
    val br = other.value
    if (br == null) {
      BackgroundPosition(offsetX = value)
    } else {
      br.copy(offsetX = value)
    }
  }

  def fromO(value: BackgroundPosition) = if (value == null) {
    null
  } else {
    value.offsetX
  }
}