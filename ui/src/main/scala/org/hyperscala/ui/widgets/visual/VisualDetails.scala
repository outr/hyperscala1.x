package org.hyperscala.ui.widgets.visual

/**
 * @author Matt Hicks <mhicks@powerscala.org>
 */
trait VisualDetails[T] {
  def clazz: Class[_]
  def itemizedType: Class[_]
  def nullAllowed: Boolean
  def default: Option[T]
  def selection: List[T]
  def masked: Boolean
}

case class VisualTypeDetails[T](clazz: Class[_],
                                itemizedType: Class[_] = null,
                                nullAllowed: Boolean = true,
                                default: Option[T] = None,
                                selection: List[T] = Nil,
                                masked: Boolean = false) extends VisualDetails[T]