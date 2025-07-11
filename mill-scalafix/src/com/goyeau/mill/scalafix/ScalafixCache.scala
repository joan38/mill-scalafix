package com.goyeau.mill.scalafix

import coursier.core.Repository
import mill.scalalib.Dep
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixArguments

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*
import scala.ref.SoftReference

private[scalafix] object ScalafixCache {

  private val scalafixCache = new Cache[(String, java.util.List[coursierapi.Repository]), Scalafix](createFunction = {
    case (scalaVersion, repositories) =>
      Scalafix.fetchAndClassloadInstance(scalaVersion, repositories)
  })

  private val scalafixArgumentsCache =
    new Cache[(String, Seq[Repository], Seq[Dep]), ScalafixArguments](createFunction = {
      case (scalaVersion, repositories, scalafixIvyDeps) =>
        val repos = repositories.map(CoursierUtils.toApiRepository).asJava
        val deps  = scalafixIvyDeps.map(CoursierUtils.toCoordinates).iterator.toSeq.asJava
        scalafixCache
          .getOrElseCreate((scalaVersion, repos))
          .newArguments()
          .withToolClasspath(Seq.empty.asJava, deps, repos)
    })

  def getOrElseCreate(scalaVersion: String, repositories: Seq[Repository], scalafixIvyDeps: Seq[Dep]) =
    scalafixArgumentsCache.getOrElseCreate((scalaVersion, repositories, scalafixIvyDeps))

  private class Cache[A, B <: AnyRef](createFunction: A => B) {
    private val cache = new ConcurrentHashMap[A, SoftReference[B]]

    def getOrElseCreate(a: A) =
      cache.compute(
        a,
        {
          case (_, v @ SoftReference(_)) => v
          case _                         => SoftReference(createFunction(a))
        }
      )()
  }
}
