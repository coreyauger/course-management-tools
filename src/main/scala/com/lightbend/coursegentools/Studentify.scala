package com.lightbend.coursegentools

/**
  * Copyright © 2016 Lightbend, Inc
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *
  * NO COMMERCIAL SUPPORT OR ANY OTHER FORM OF SUPPORT IS OFFERED ON
  * THIS SOFTWARE BY LIGHTBEND, Inc.
  *
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

import com.typesafe.config.ConfigFactory

object Studentify {

  def main(args: Array[String]): Unit = {

    import Helpers._
    import java.io.File
    import sbt.io.{IO => sbtio}

    implicit val exitOnFirstError: ExitOnFirstError = ExitOnFirstError(true)

    val cmdOptions = StudentifyCmdLineOptParse.parse(args)
    if (cmdOptions.isEmpty) System.exit(-1)
    val StudentifyCmdOptions(masterRepo, targetFolder, multiJVM, firstOpt, lastOpt, selectedFirstOpt, configurationFile, useConfigureForProjects) = cmdOptions.get

    exitIfGitIndexOrWorkspaceIsntClean(masterRepo)
    val projectName = masterRepo.getName
    val targetCourseFolder = new File(targetFolder, projectName)

    val tmpDir = cleanMasterViaGit(masterRepo, projectName)
    val cleanMasterRepo = new File(tmpDir, projectName)

    implicit val config: MasterSettings = new MasterSettings(masterRepo, configurationFile)
    import config.testCodeFolders, config.studentifyModeClassic.studentifiedBaseFolder

    val exercises: Seq[String] = getExerciseNames(cleanMasterRepo, Some(masterRepo))

    val selectedExercises: Seq[String] = getSelectedExercises(exercises, firstOpt, lastOpt)
    println(
      s"""Processing exercises:
         |${selectedExercises.mkString("    ", "\n    ", "")}
       """.stripMargin)
    val initialExercise = getInitialExercise(selectedFirstOpt, selectedExercises)
    val sbtStudentCommandsTemplateFolder = new File("sbtStudentCommands")
    stageFirstExercise(initialExercise, new File(cleanMasterRepo, config.relativeSourceFolder), targetCourseFolder)
    copyMaster(cleanMasterRepo, targetCourseFolder)
    hideExerciseSolutions(targetCourseFolder, selectedExercises)
    createBookmarkFile(initialExercise, targetCourseFolder)
    createSbtRcFile(targetCourseFolder)
    createStudentifiedBuildFile(targetCourseFolder, multiJVM)
    addSbtStudentCommands(sbtStudentCommandsTemplateFolder, targetCourseFolder)
    loadStudentSettings(masterRepo, targetCourseFolder)
    cleanUp(config.studentifyFilesToCleanUp, targetCourseFolder)
    sbtio.delete(tmpDir)

  }

}
