package material.plugin

import com.amazonaws.auth.BasicAWSCredentials
import com.thoughtworks.go.plugin.api.material.packagerepository._
import com.thoughtworks.go.plugin.api.response.Result
import material.plugin.config.S3PackageMaterialConfiguration
import material.store._
import com.amazonaws.services.s3.AmazonS3Client
import material.store.Exists
import material.store.S3ArtifactStore
import material.util.LoggerUtil


class S3PackageMaterialPoller extends PackageMaterialPoller with LoggerUtil {
  val USER = "go"
  override def getLatestRevision(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): PackageRevision = {
    val s3Bucket = repoConfig.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue
    val artifactStore = S3ArtifactStore(s3Client(repoConfig), s3Bucket)
    val revision = artifactStore.latestRevision(artifact(packageConfig))
    revision match {
      case x: RevisionSuccess => new PackageRevision(x.revision.revision, x.lastModified, USER, x.revisionComments, x.trackBackUrl)
      case f : OperationFailure => throw new RuntimeException(f.th)
     }
  }

  override def checkConnectionToPackage(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): Result = {
    val s3Bucket = repoConfig.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue
    val artifactStore = S3ArtifactStore(s3Client(repoConfig), s3Bucket)
    artifactStore.exists(artifact(packageConfig).prefix) match {
      case e: Exists => new Result().withSuccessMessages(s"Check ${artifact(packageConfig)} exists ${e.message}")
      case f: OperationFailure => 
	f.th.printStackTrace()
	new Result().withErrorMessages(f.message)
    }
  }

  override def checkConnectionToRepository(repoConfig: RepositoryConfiguration): Result = {
    val s3Bucket = repoConfig.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue
    val artifactStore = S3ArtifactStore(s3Client(repoConfig), s3Bucket)
    artifactStore.bucketExists match {
      case e: Exists => new Result().withSuccessMessages(s"Check [$s3Bucket] exists ${e.message}")
      case f: OperationFailure => new Result().withErrorMessages(f.message)
    }
  }

  override def latestModificationSince(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration, lastKnownRevision: PackageRevision): PackageRevision = {
    // S3 doesn't seem to provide APIs to pull pegged updates
    // This means, we need to do a getLatest for this artifact anyways
    // Finally check to see if the latest revision is newer than the incoming revision
    // and return PackageRevision instance appropriately.
    val packageRevision = getLatestRevision(packageConfig, repoConfig)
    if(Revision(packageRevision.getRevision).compare(Revision(lastKnownRevision.getRevision)) > 0)
      packageRevision
    else
      null
  }

  private def s3Client(repoConfig: RepositoryConfiguration) : AmazonS3Client = {
    val accessKey = repoConfig.get(S3PackageMaterialConfiguration.S3_ACCESS_KEY_ID).getValue
    val secretKey = repoConfig.get(S3PackageMaterialConfiguration.S3_SECRET_ACCESS_KEY).getValue
    new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey))
  }
  
  def artifact(packageConfig: PackageConfiguration) = {
    val pipelineName = packageConfig.get(S3PackageMaterialConfiguration.PIPELINE_NAME).getValue
    val stageName = packageConfig.get(S3PackageMaterialConfiguration.STAGE_NAME).getValue
    val jobName = packageConfig.get(S3PackageMaterialConfiguration.JOB_NAME).getValue
    Artifact(pipelineName, stageName,jobName)
  }
}
