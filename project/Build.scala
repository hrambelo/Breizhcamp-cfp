import sbt._
import Keys._
import PlayProject._
import cloudbees.Plugin._

object ApplicationBuild extends Build {

    import Tasks._

    val appName         = "BreizhCamp-cfp"
    val appVersion      = "1.1"

    val appDependencies = Seq(
      "com.typesafe" %% "play-plugins-mailer" % "2.0.2",
      "be.objectify"  %%  "deadbolt-2"        % "1.1.3-SNAPSHOT",
      "com.feth"      %%  "play-authenticate" % "0.2.2-SNAPSHOT",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "mysql" % "mysql-connector-java" % "5.1.21"
    )

    lazy val s = Defaults.defaultSettings ++ Seq(generateAPIDocsTask)

    //  Uncomment this for local development of the Play Authenticate core:
    /*
        val playAuthenticate = PlayProject(
          "play-authenticate", "1.0-SNAPSHOT", mainLang = JAVA, path = file("modules/play-authenticate")
        ).settings(
          libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.2",
          libraryDependencies += "com.feth" %% "play-easymail" % "0.1-SNAPSHOT",
          libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m",
          libraryDependencies += "commons-lang" % "commons-lang" % "2.6",

          resolvers += Resolver.url("play-easymail (release)", url("http://joscha.github.com/play-easymail/repo/releases/"))(Resolver.ivyStylePatterns),
          resolvers += Resolver.url("play-easymail (snapshot)", url("http://joscha.github.com/play-easymail/repo/snapshots/"))(Resolver.ivyStylePatterns)
        )
    */


    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA, settings = s)
    .settings(cloudBeesSettings :_*)
    .settings(CloudBees.applicationId := Some("breizhcamp/cfp")).settings(
            // Add your own project settings here
           resolvers += Resolver.url("Objectify Play Repository", url("http://schaloner.github.com/releases/"))(Resolver.ivyStylePatterns),
           resolvers += Resolver.url("Objectify Play Repository", url("http://schaloner.github.com/snapshots/"))(Resolver.ivyStylePatterns),

           resolvers += Resolver.url("play-easymail (release)", url("http://joscha.github.com/play-easymail/repo/releases/"))(Resolver.ivyStylePatterns),
           resolvers += Resolver.url("play-easymail (snapshot)", url("http://joscha.github.com/play-easymail/repo/snapshots/"))(Resolver.ivyStylePatterns),

           resolvers += Resolver.url("play-authenticate (release)", url("http://joscha.github.com/play-authenticate/repo/releases/"))(Resolver.ivyStylePatterns),
           resolvers += Resolver.url("play-authenticate (snapshot)", url("http://joscha.github.com/play-authenticate/repo/snapshots/"))(Resolver.ivyStylePatterns),
           resolvers += "Apache" at "http://repo1.maven.org/maven2/",
           resolvers += "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/"
    )

    //  Uncomment this for local development of the Play Authenticate core:
    //    .dependsOn(playAuthenticate).aggregate(playAuthenticate)

    object Tasks {

        val generateAPIDocsTask = TaskKey[Unit]("app-doc") <<= (fullClasspath in Test, compilers, streams) map { (classpath, cs, s) => 
        
          IO.delete(file("documentation/api"))
          // Scaladoc
          var scalaVersionForSbt = Option(System.getProperty("scala.version")).getOrElse("2.9.1")

          val sourceFiles = 
            (file("app") ** "*.scala").get ++ 
            (file("test") ** "*.scala").get ++ 
            (file("target/scala-" + scalaVersionForSbt + "/src_managed/main/views/html") ** "*.scala").get
          new Scaladoc(10, cs.scalac)(appName + " " + appVersion + " Scala API", sourceFiles, classpath.map(_.data), file("documentation/api/scala"), Nil, s.log)
          
          // Javadoc
          val javaSources = Seq(file("app"), file("test")).mkString(":")
          val javaApiTarget = file("documentation/api/java")
          val javaClasspath = classpath.map(_.data).mkString(":")
          val javaPackages = "controllers:models"

          val cmd = <x>javadoc -windowtitle {appName} -doctitle {"\"" + appName + "&nbsp;" + appVersion + "&nbsp;Java&nbsp;API\""} -sourcepath {javaSources} -d {javaApiTarget} -subpackages {javaPackages} -classpath {javaClasspath}</x>
          //println("Executing: "+cmd.text)
          cmd ! s.log

          println(appName + " " + appVersion + " Documentation generated under documentation/api/ ")
        }
    }


}


      
      
      

      
