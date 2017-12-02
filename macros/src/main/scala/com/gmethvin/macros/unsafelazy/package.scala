package com.gmethvin.macros

package object unsafelazy {

  import scala.meta._
  import scala.collection.immutable.Seq

  class lazyInit extends scala.annotation.StaticAnnotation

  class UnsafeLazy extends scala.annotation.StaticAnnotation {

    inline def apply(defn: Any): Any = meta {

      object UnsafeLazyMods {
        def unapply(mods: Seq[Mod]): Option[Seq[Mod]] = {
          @inline def isLazy(mod: Mod) = mod match {
            case mod"@lazyInit" => true
            case _ => false
          }
          if (mods.exists(isLazy)) {
            Some(mods.filterNot(isLazy))
          } else None
        }
      }

      val bitmap = Term.fresh(s"_lazy_bitmap_") // variable to store initialization status

      def expand(stats: Seq[Stat]): Seq[Stat] = {
        var bits: Int = 0 // number of bits
        // helper methods for setting the bit at the given position and check if if the bit is empty
        // these are @inline so they should be optimized away
        val setBit = Term.fresh("_lazy_setBit_")
        val bitIsEmpty = Term.fresh("_lazy_bitIsEmpty_")

        val newStats = stats.flatMap {
          case pat @ q"..${UnsafeLazyMods(mods)} val ..$patsnel: $tpeopt = $expr" =>
            patsnel.flatMap {
              case Pat.Var.Term(tname @ Term.Name(strname)) =>
                val bit = bits
                bits += 1
                val storevar = Term.fresh("_lazy_") // variable to store data
                tpeopt match {
                  case Some(tpe) =>
                    var shifted = Term.fresh("_shifted_bitmap_")
                    var body =
                      q"""{
                       val ${Pat.Var.Term(shifted)} = 1L << $bit
                       if (($bitmap & $shifted) == 0) {
                         $storevar = $expr
                         $bitmap |= $shifted
                       }
                       $storevar
                     }"""
                    Seq(
                      q"private[this] final var ${Pat.Var.Term(storevar)}: $tpe = _",
                      q"..$mods def $tname: $tpeopt = $body",
                    )
                  case None =>
                    abort(s"UnsafeLazy only works on lazy vals with explicit types. See: $pat")
                }
              case otherpat =>
                abort(s"Must be a term name: $otherpat")
            }
          case pat @ q"..${UnsafeLazyMods(mods)} val ..$patsnel: $tpeopt = $expr" =>
            abort(s"didn't match $pat")
          case other => Seq(other)
        }
        if (bits > 0) {
          if (bits > 64) {
            abort(s"Sorry, no more than 64 @lazyInit vals supported!")
          }
          val extraStats = Seq(
            q"private[this] final var ${Pat.Var.Term(bitmap)}: Long = 0L"
          )

          extraStats ++ newStats
        } else {
          newStats
        }
      }

      val result = defn match {
        case q"..$mods object $ename extends ..$templates { ..$stats }" =>
          q"..$mods object $ename extends ..$templates { ..${expand(stats)} }"
        case q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends ..$templates { ..$stats }" =>
          q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends ..$templates { ..${expand(stats)} }"
        case q"..$mods trait $tname[..$tparams] extends ..$templates { ..$stats }" =>
          q"..$mods trait $tname[..$tparams] extends ..$templates { ..${expand(stats)} }"
        case other =>
          other
      }

      println(result)

      result
    }

  }

}
