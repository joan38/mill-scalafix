package com.goyeau.mill.scalafix

import coursier.Repository
import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.core.Authentication
import mill.scalalib.{CrossVersion, Dep}

object CoursierUtils {
  def toApiRepository(repo: Repository): Option[coursierapi.Repository] =
    repo match {
      case mvn: MavenRepository =>
        val credentialsOpt = mvn.authentication.map(toApiCredentials)
        val apiRepo = coursierapi.MavenRepository
          .of(mvn.root)
          .withCredentials(credentialsOpt.orNull)
        Some(apiRepo)
      case ivy: IvyRepository =>
        val credentialsOpt = ivy.authentication.map(toApiCredentials)
        val mdPatternOpt   = ivy.metadataPatternOpt.map(_.string)
        val apiRepo = coursierapi.IvyRepository
          .of(ivy.pattern.string)
          .withMetadataPattern(mdPatternOpt.orNull)
          .withCredentials(credentialsOpt.orNull)
        Some(apiRepo)
      case _ =>
        // non-standard repository, ignoring it
        None
    }

  def toApiCredentials(auth: Authentication): coursierapi.Credentials =
    coursierapi.Credentials.of(auth.user, auth.passwordOpt.getOrElse(""))

  def toCoordinates(dep: Dep): String =
    dep.cross match {
      case CrossVersion.Constant(value, _) =>
        s"${dep.dep.module.organization.value}:${dep.dep.module.name.value}$value:${dep.dep.version}"
      case CrossVersion.Binary(_) =>
        s"${dep.dep.module.organization.value}::${dep.dep.module.name.value}:${dep.dep.version}"
      case CrossVersion.Full(_) =>
        s"${dep.dep.module.organization.value}:::${dep.dep.module.name.value}:${dep.dep.version}"
    }
}
