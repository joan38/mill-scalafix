package com.goyeau.mill.scalafix

import coursier.core.Repository
import mill.scalalib.Dep
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixArguments

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*
import scala.ref.SoftReference

private[scalafix] object ScalafixCache:

  private val scalafixCache = new Cache[(String, java.util.List[coursierapi.Repository]), Scalafix](createFunction = {
    case (scalaVersion, repositories) =>
      Scalafix.fetchAndClassloadInstance(scalaVersion, repositories)
  })

  private val scalafixArgumentsCache =
    new Cache[(String, Seq[Repository], Seq[Dep], Seq[os.Path]), ScalafixArguments](createFunction = {
      case (scalaVersion, repositories, scalafixIvyDeps, scalafixToolClasspath) =>
        val repos    = repositories.map(CoursierUtils.toApiRepository).asJava
        val deps     = scalafixIvyDeps.map(CoursierUtils.toCoordinates).iterator.toSeq.asJava
        val toolUrls = scalafixToolClasspath.map(_.toNIO.toAbsolutePath.toUri.toURL).asJava
        scalafixCache
          .getOrElseCreate((scalaVersion, repos))
          .newArguments()
          .withToolClasspath(toolUrls, deps, repos)
    })

  def getOrElseCreate(
      scalaVersion: String,
      repositories: Seq[Repository],
      scalafixIvyDeps: Seq[Dep],
      scalafixToolClasspath: Seq[os.Path]
  ) =
    scalafixArgumentsCache.getOrElseCreate((scalaVersion, repositories, scalafixIvyDeps, scalafixToolClasspath))

  private class Cache[A, B <: AnyRef](createFunction: A => B):
    private val cache = new ConcurrentHashMap[A, SoftReference[B]]

    def getOrElseCreate(a: A) =
      cache.compute(
        a,
        {
          case (_, v @ SoftReference(_)) => v
          case _                         => SoftReference(createFunction(a))
        }
      )()
