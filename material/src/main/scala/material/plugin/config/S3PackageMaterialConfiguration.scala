package material.plugin.config

import com.thoughtworks.go.plugin.api.material.packagerepository._
import com.thoughtworks.go.plugin.api.response.validation.{ValidationError, ValidationResult}
import com.thoughtworks.go.plugin.api.config.{Configuration, Property}

import scala.collection.JavaConverters._
import org.apache.commons.lang3.StringUtils
import material.util.LoggerUtil

case class Config(name: String, displayName: String, order: Int, required: java.lang.Boolean = true) {
  def toPackageProperty = new PackageMaterialProperty(name)
    .`with`[String](Property.DISPLAY_NAME, displayName)
    .`with`[Integer](Property.DISPLAY_ORDER, order)
    .`with`[java.lang.Boolean](Property.REQUIRED, required)
}

object S3PackageMaterialConfiguration {
  val S3_BUCKET = "S3_BUCKET"
  val S3_ACCESS_KEY_ID = "S3_AWS_ACCESS_KEY_ID"
  val S3_SECRET_ACCESS_KEY = "S3_AWS_SECRET_ACCESS_KEY"
  val PIPELINE_NAME = "PIPELINE_NAME"
  val STAGE_NAME = "STAGE_NAME"
  val JOB_NAME = "JOB_NAME"
  
  val repoConfigs = List(
    Config(S3_BUCKET, "S3 Bucket", 0),
    Config(S3_ACCESS_KEY_ID, "S3 Access Key ID", 1),
    Config(S3_SECRET_ACCESS_KEY, "S3 Secret Access Key", 2)
  )
  
  val packageConfigs = List(
    Config(PIPELINE_NAME, "Pipeline Name", 0),
    Config(STAGE_NAME, "Stage Name", 1),
    Config(JOB_NAME, "Job Name", 2)
  )
}

class S3PackageMaterialConfiguration extends PackageMaterialConfiguration  with LoggerUtil {
  override def getRepositoryConfiguration: RepositoryConfiguration = {
    val repoConfig = new RepositoryConfiguration()
    S3PackageMaterialConfiguration.repoConfigs.map(_.toPackageProperty).foreach(p => repoConfig.add(p))
    repoConfig
  }

  def validate(config: Configuration, property: String, message: String, required: Boolean) = {
    if(required && (config.get(property) == null || StringUtils.isBlank(config.get(property).getValue))) {
      val validationResult = new ValidationResult()
      validationResult.addError(new ValidationError(property, message))
      Some(validationResult)
    }else{
      None
    }
  }

  override def isRepositoryConfigurationValid(repoConfig: RepositoryConfiguration): ValidationResult = {
    val errors = S3PackageMaterialConfiguration.repoConfigs
      .map(c => validate(repoConfig,c.name, s"${c.name} configuration is missing or value is empty", c.required))
      .flatMap(vr => vr.map(_.getErrors.asScala).getOrElse(List[ValidationError]()))

    val validationResult = new ValidationResult()
    validationResult.addErrors(errors.asJava)
    validationResult
  }

  override def isPackageConfigurationValid(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): ValidationResult = {
    val errors = S3PackageMaterialConfiguration.packageConfigs
      .map(c => validate(packageConfig,c.name, s"${c.name} configuration is missing or value is empty", c.required))
      .flatMap(vr => vr.map(_.getErrors.asScala).getOrElse(List[ValidationError]()))
    val validationResult = new ValidationResult()
    validationResult.addErrors(errors.asJava)
    validationResult
  }

  override def getPackageConfiguration: PackageConfiguration = {
    val packageConfig = new PackageConfiguration()
    S3PackageMaterialConfiguration.packageConfigs.map(_.toPackageProperty).foreach(p => packageConfig.add(p))
    packageConfig
  }
}