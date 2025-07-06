package com.goyeau.mill.scalafix

import coursier.Repository
import coursier.core.Authentication
import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import mill.api.CrossVersion
import mill.scalalib.Dep

object CoursierUtils {
  def toApiRepository(repo: Repository): coursierapi.Repository =
    repo match {
      case mvn: MavenRepository =>
        val credentialsOpt = mvn.authentication.map(toApiCredentials)
        coursierapi.MavenRepository
          .of(mvn.root)
          .withCredentials(credentialsOpt.orNull)
      case ivy: IvyRepository =>
        val credentialsOpt = ivy.authentication.map(toApiCredentials)
        val mdPatternOpt   = ivy.metadataPatternOpt.map(_.string)
        coursierapi.IvyRepository
          .of(ivy.pattern.string)
          .withMetadataPattern(mdPatternOpt.orNull)
          .withCredentials(credentialsOpt.orNull)
      case other =>
        throw new Exception(s"Unrecognized repository: $other")
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
