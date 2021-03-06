package org.hyperscala.io.bootstrap

import org.hyperscala.html.HTMLTag
import org.hyperscala.io.{ScalaBuffer, WriterContext}

/**
 * Takes an [[HTMLTag]] and checks whether it is a valid Bootstrap component.
 * Bootstrap components are translated to their Scala representation. All
 * known components are specified in [[Specification.components]].
 */
object Generation {
  import Specification._

  def findComponent(tag: HTMLTag, parentComponent: Option[String] = None): Option[Component] =
    components.find { component =>
      tagMatches(tag, component) &&
        cssMatches(tag, component) &&
        (component.parentComponent.isEmpty ||
          component.parentComponent == parentComponent)
    }

  def tagMatches(tag: HTMLTag, component: Component): Boolean =
    component.tag
      .map(t => (s: String) => s == t)
      .getOrElse((_: String) => true)(tag.xmlLabel)

  /** If `tag` contains more CSS classes than specified, we cannot convert it. */
  def cssMatches(tag: HTMLTag, component: Component): Boolean = {
    val classes = tag.clazz().toSet

    val allClassesCovered = component match {
      case c @ DefComponent(name, parent, t, attributes, cssF, properties @ _*) =>
        cssF(classes) match {
          case None => false
          case Some(matches) =>
            classes.diff(matches).forall(t => properties.exists { property =>
              property.values.exists {
                case v @ Value.Boolean(css)
                  if t == css => true
                case v @ Value.Set(name, options @ _*)
                  if options.exists(_.css == t) => true
                case v @ Value.Enum(values, css)
                  if values.map(css).contains(t) => true
                case v @ Value.Option(name, css)
                  if t == css => true
                case _ => false
              }
            })
        }

      case EnumComponent(name, parent, t, css, values @ _*) =>
        css(classes) match {
          case None => false
          case Some(matches) =>
            classes.diff(matches).forall(t => values.exists(_.css == t))
        }
    }

    allClassesCovered
  }

  def extractProperties(tag: HTMLTag, component: DefComponent): Seq[(Property, Value)] = {
    component.properties.map { property =>
      val value: Option[Value] = property.values.collectFirst {
        case v @ Value.Boolean(css)
          if tag.clazz().contains(css) => v
        case v @ Value.Set(name, options @ _*)
          if options.exists(o => tag.clazz().contains(o.css)) =>
            options.find(o => tag.clazz().contains(o.css)).get
        case v @ Value.Enum(values, css)
          if values.map(css).exists(tag.clazz().contains(_)) =>
            Value.Instance(values.find(v => tag.clazz().contains(css(v))).get)
        case v @ Value.Option(name, css)
          if tag.clazz().contains(css) => v
      }

      value.map((property, _))
    }.collect { case Some(x) => x }
  }

  def encodePropertyValue(tag: HTMLTag, property: Property, value: Value): String = {
    value match {
      case Value.Boolean(_) => "true"
      case Value.Set(name, options @ _*) =>
        val option = options.find(o => tag.clazz().contains(o.css)).get
        s"bootstrap.component.$name.${option.name}"
      case Value.Enum(values, css) =>
        val v = values.map(css).find(tag.clazz().contains(_)).get
        s"bootstrap.component.$v"
      case Value.Option(name, css) =>
        val propName = property.values.head.asInstanceOf[Value.Set].name
        s"bootstrap.component.$propName.$name"
      case Value.Instance(v) => s"Some($v)"
    }
  }

  def applyComponent(tag: HTMLTag,
                     component: Component,
                     code: WriterContext,
                     vals: WriterContext,
                     mapping: Map[String, String]) {
    component match {
      case c @ DefComponent(name, parent, t, attributes, css, properties @ _*) =>
        val props = extractProperties(tag, c).map { case (p, v) =>
          val valueTree = encodePropertyValue(tag, p, v)
          s"${p.name} := $valueTree"
        }

        if (t.nonEmpty) code.writeLine(s"new bootstrap.component.$name {", indent = false)
        else {
          val scalaTag = tag.xmlLabel.head.toUpper + tag.xmlLabel.tail
          code.writeLine(s"new tag.$scalaTag with bootstrap.component.$name {", indent = false)
        }

        code.depth += 1
        ScalaBuffer.writeAttributes(tag, all = true, code = code,
          withoutAttributes = Set("clazz") ++ attributes)
        props.foreach(code.writeLine(_))
        ScalaBuffer.writeChildren(tag, code, vals, mapping, Some(component.name))
        code.depth -= 1
        code.writeLine(s"}")

      case EnumComponent(name, parent, t, fTag, values @ _*) =>
        val opt = values.find(v => tag.clazz().contains(v.css)).get
        code.writeLine(s"bootstrap.component.$name.${opt.name}", indent = false)
    }
  }
}