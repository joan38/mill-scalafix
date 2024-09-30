package com.goyeau.mill.scalafix

import com.goyeau.mill.scalafix.CoursierUtils
import coursier.core.Repository
import scalafix.interfaces.Scalafix

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._
import scala.ref.SoftReference

private[scalafix] object ScalafixCache {

  private val cache = new ConcurrentHashMap[(String, Seq[Repository]), SoftReference[Scalafix]]

  def getOrElseCreate(scalaVersion: String, repositories: Seq[Repository]) =
    cache.compute(
      (scalaVersion, repositories),
      {
        case (_, v @ SoftReference(_)) => v
        case _ =>
          SoftReference(
            Scalafix.fetchAndClassloadInstance(scalaVersion, repositories.map(CoursierUtils.toApiRepository).asJava)
          )
      }
    )()
}
