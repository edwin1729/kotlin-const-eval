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
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.math.exp


class ConstSubsTransformer(private val pluginContext: IrPluginContext): IrElementTransformer<Boolean> {
  private var evalFunCorrect: Boolean = true // only meaningful when inside an eval function or just came out of one
  private val workingDir = Path("/homes/ef322/temp")

  override fun visitModuleFragment(declaration: IrModuleFragment, data: Boolean): IrModuleFragment {
    val foo = super.visitModuleFragment(declaration, data)
    println(declaration.dumpKotlinLike())
    return foo
  }

  private fun isPrimitives(expression: IrCall): Boolean {
    val callerType = expression.dispatchReceiver?.type ?: expression.extensionReceiver?.type
    return expression.type.isPrimitiveType()
      // check if caller is a primitive
      && callerType != null && callerType.isPrimitiveType()
      // no generics are allowed
      && expression.typeArgumentsCount > 0 &&
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
    val evalScript = File(workingDir.name, fileName)
    evalScript.createNewFile()
    evalScript.writeText(irCall.dumpKotlinLike())
    evalScript.appendText(irFunDef.dumpKotlinLike())
    val output = "kotlinc -script $fileName".runCommand(workingDir.toFile()) ?: return irCall
    return when (irCall.type) {
      pluginContext.irBuiltIns.intType -> IrConstImpl.int(0, 0, irCall.type, output.toInt())
      pluginContext.irBuiltIns.stringType -> IrConstImpl.string(0, 0, irCall.type, output)
      pluginContext.irBuiltIns.booleanType -> IrConstImpl.boolean(0, 0, irCall.type, output.toBoolean())
      // TODO Other primitves
      else -> irCall
    }
  }

  private fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
      assert(false)
        return "fail"
    }
}
}
