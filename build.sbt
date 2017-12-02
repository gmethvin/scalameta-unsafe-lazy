
scalaVersion in ThisBuild := "2.12.4"

lazy val macroAnnotationSettings = Seq(
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full),
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) ~= (_ filterNot (_ contains "paradise")) // macroparadise plugin doesn't work in repl yet.
)

lazy val macros = project.settings(
  macroAnnotationSettings,
  libraryDependencies += "org.scalameta" %% "scalameta" % "1.8.0"
)

// Use macros in this project.
lazy val app = project.settings(macroAnnotationSettings).dependsOn(macros)
