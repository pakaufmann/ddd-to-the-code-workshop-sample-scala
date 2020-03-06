package com.github.pkaufmann.dddttc.stereotypes

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.api.Trees
import scala.reflect.macros.blackbox

@compileTimeOnly("Set -Ymacro-annotations")
class Aggregate extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AggregateMacros.impl
}

class AggregateId extends StaticAnnotation

object AggregateMacros {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def containsParam(applyParams: List[ValDef], param: ValDef): Boolean = {
      applyParams.exists {
        case q"$paramMods val $name: $tpt = $unused" if name == param.name && tpt.equalsStructure(param.tpt) =>
          true
        case _ =>
          false
      }
    }

    def findAggregateId(fields: List[ValDef]) = {
      fields
        .find(a => {
          a.mods.annotations.exists(_.asInstanceOf[Apply].fun.asInstanceOf[Select].qualifier.asInstanceOf[New].tpt.asInstanceOf[Ident].name == typeOf[AggregateId].typeSymbol.name)
        })
        .getOrElse(c.abort(c.enclosingPosition, "@Aggregate needs a field annotated with @AggregateId"))
        .asInstanceOf[ValDef]
        .name
    }

    def overrideDefaultApply(className: c.TypeName, objBody: Seq[Trees#Tree], firstParamList: List[c.universe.ValDef], names: List[c.universe.Tree]) = {
      val defaultApplyExists = objBody.exists {
        case q"$applyMods def apply(...$params): $className = $expr" if firstParamList.forall(f => containsParam(params.head.asInstanceOf[List[ValDef]], f)) => true
        case _ => false
      }

      if(!defaultApplyExists) {
        val applyParams = firstParamList.map(v => q"val ${v.name}: ${v.tpt}")
        q"private def apply(..$applyParams): $className = new ${className}(..$names)"
      } else {
        q""
      }
    }

    val result = {
      annottees.map(_.tree).toList match {
        case q"case class $className ..$constructorMods (...$fields) { ..$body }" :: q"$objMods object $objName { ..$objBody }" :: Nil => {
          val firstParamList = fields.head.asInstanceOf[List[ValDef]]
          val aggregateId = findAggregateId(firstParamList)

          val names = firstParamList.map(v => q"${v.name}")
          val copyParams = firstParamList.map(v => q"val ${v.name}: ${v.tpt} = this.${v.name}")

          val privateDefaultApply = overrideDefaultApply(className.asInstanceOf[c.TypeName], objBody, firstParamList, names)

          q"""case class $className $constructorMods(...$fields) {
            ..$body

            private def copy(..$copyParams): $className = {
               ${className.asInstanceOf[TypeName].encodedName.toTermName}.apply(..$names)
            }

            override def hashCode(): Int = {
              scala.util.hashing.MurmurHash3.seqHash(scala.collection.immutable.Seq(${aggregateId}))
            }

            override def equals(obj: Any): Boolean = {
              obj match {
                case o: $className =>
                  o.${aggregateId}.equals(${aggregateId})
                case _ => false
              }
            }
          }

          $objMods object $objName {
              implicit val ___generic = shapeless.Generic[${className.asInstanceOf[TypeName].encodedName.toTypeName}]
              implicit val ___labelledGeneric = shapeless.LabelledGeneric[${className.asInstanceOf[TypeName].encodedName.toTypeName}]

              ..$objBody
              $privateDefaultApply
          }
          """
        }
        case _ => c.abort(c.enclosingPosition, "Annotation @Aggregate can be used only on case classes which have an explicit companion object")
      }
    }

    c.Expr[Any](result)
  }

}