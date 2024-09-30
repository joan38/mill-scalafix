package com.goyeau.mill.scalafix

import com.goyeau.mill.scalafix.CoursierUtils
import coursier.core.Repository
import scalafix.interfaces.Scalafix

import scala.collection.mutable
import scala.ref.SoftReference
import scala.jdk.CollectionConverters._

private[scalafix] object ScalafixCache {

  private val cache = mutable.Map.empty[(String, Seq[Repository]), SoftReference[Scalafix]]

  def getOrElseCreate(scalaVersion: String, repositories: Seq[Repository]) =
    cache.get((scalaVersion, repositories)) match {
      case Some(SoftReference(value)) => value
      case _ =>
        val newResult =
          Scalafix.fetchAndClassloadInstance(scalaVersion, repositories.map(CoursierUtils.toApiRepository).asJava)
        cache.update((scalaVersion, repositories), SoftReference(newResult))
        newResult
    }
}
