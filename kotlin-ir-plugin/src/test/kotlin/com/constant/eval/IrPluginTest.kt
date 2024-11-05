/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.constant.eval

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import kotlin.test.assertEquals

class IrPluginTest {

  @Test
  fun nestedEval() {
    println("Here is the transformed IR")
    // Printing happens as side effect. Ideally I look into the compiler-test-library api to see how get the IR here in the test
    // rather than as part of implementation
    val result = compile(
      sourceFile = SourceFile.kotlin(
        "main.kt", """
fun main() {
  println(evalDebug())
}

fun evalDebug(): Int {
    val foo = evalId("5".toInt())
    return foo
}
fun evalId(x: Int) = x
"""
      )
    )
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
  }

  @Test
  fun simpleFunction() {
    val result = compile(
      sourceFile = SourceFile.kotlin(
        "main.kt", """
fun main() {
  println(evalDebug())
}

fun evalDebug(): Int {
    var bar = 6
    bar += 10
    return bar
}
"""
      )
    )

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
  }


  fun compile(
    sourceFiles: List<SourceFile>,
    plugin: CompilerPluginRegistrar = ConstEvalCompilerRegistrar(),
  ): JvmCompilationResult {
    return KotlinCompilation().apply {
      sources = sourceFiles
      compilerPluginRegistrars = listOf(plugin)
      inheritClassPath = true
    }.compile()
  }

  fun compile(
    sourceFile: SourceFile,
    plugin: CompilerPluginRegistrar = ConstEvalCompilerRegistrar(),
  ): JvmCompilationResult {
    return compile(listOf(sourceFile), plugin)
  }
}
