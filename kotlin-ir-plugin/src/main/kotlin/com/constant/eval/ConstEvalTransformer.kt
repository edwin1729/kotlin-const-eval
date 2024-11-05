package com.constant.eval

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory

class ConstEvalTransformer(private val pluginContext: IrPluginContext): IrElementTransformer<Boolean> {
  private var evalFunCorrect: Boolean = true // only meaningful when inside an eval function or just came out of one
  private val workingDir = createTempDirectory()

  // Solely for the purpose of printing the transformed IR as a side effect. This was just for debugging,
  // but I ran out of time to figure to properly look into `com.tschuchort.compiletesting` which is library used for testing
  // So I just print the transformed IR when running the test to show it works
  override fun visitModuleFragment(declaration: IrModuleFragment, data: Boolean): IrModuleFragment {
    val foo = super.visitModuleFragment(declaration, data)
    println(declaration.dumpKotlinLike())
    return foo
  }

  private fun isPrimitives(expression: IrCall): Boolean {
    // val callerType = expression.dispatchReceiver?.type ?: expression.extensionReceiver?.type
    // (callerType == null || callerType.isPrimitiveType() || callerType.isSimpleType)
    // the above condition was one of the conjuncts before. For some reason callerType.isSimpleType throws
    // an IllegalArgument Exception for functions of type `simpleTypeImpl` such as `toInt`
    // as a consequence have user defined classes as the receiver type will be erroneously allowed by the checker

    // no generics are allowed
    return expression.typeArgumentsCount == 0 &&
      // whether arguments are primitive
      // disallow varargs, and default values in eval functions since the type is not known at this point
      expression.valueArguments.all { it?.type?.isPrimitiveType() ?: false }
  }

  override fun visitCall(expression: IrCall, insideEvalFun : Boolean): IrElement {
    super.visitCall(expression, insideEvalFun)
    val isEval = expression.symbol.owner.name.asString().startsWith("eval")
    if ((isEval || insideEvalFun) && evalFunCorrect && isPrimitives(expression)) {
      if (isEval) {
        expression.symbol.owner.accept(this, true)
        return if (evalFunCorrect) {
          evaluateUsingKotlinc(expression, expression.symbol.owner)
        } else {
          expression
        }
      } else { // this is within an eval function
        evalFunCorrect = true
        return expression
      }
    } else {
      evalFunCorrect = false
        return expression
    }
  }

  private fun evaluateUsingKotlinc(irCall: IrCall, irFunDef: IrFunction): IrElement {
    val fileName = "exec.kts"
    val evalScript = File(workingDir.toString(), fileName)
    evalScript.createNewFile()

    // wrap the call with a print, so the output of the subprocess can be retrieved from subprocess
    evalScript.writeText("println(${irCall.dumpKotlinLike()})\n${irFunDef.dumpKotlinLike()}")
    val output = "kotlinc -script $fileName".runCommand(workingDir.toFile()) ?: return irCall
    return when (irCall.type) {
      // Ideally this when statement is replaced with some reflection / other type level magic converting
      // from the companion objects defined in the sealed class `IrConst` to the corresponding `to*()` function (eg. `toInt()`)
      pluginContext.irBuiltIns.intType -> IrConstImpl.int(0, 0, irCall.type, output.toInt())
      pluginContext.irBuiltIns.stringType -> IrConstImpl.string(0, 0, irCall.type, output)
      pluginContext.irBuiltIns.booleanType -> IrConstImpl.boolean(0, 0, irCall.type, output.toBoolean())
      // TODO Other primitves
      else -> irCall
    }
  }

  // from https://stackoverflow.com/a/41495542
  private fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

      proc.waitFor()
      return if (proc.exitValue() != 0) {
        null
      } else {
        proc.inputStream.bufferedReader().readText().removeSuffixIfPresent("\n")
      }
    } catch(e: IOException) {
        e.printStackTrace()
      return null
    }
  }
}
