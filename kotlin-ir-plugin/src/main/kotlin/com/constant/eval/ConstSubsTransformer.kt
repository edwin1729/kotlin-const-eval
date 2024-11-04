package com.constant.eval

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
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


class ConstSubsTransformer(private val pluginContext: IrPluginContext): IrElementTransformer<Boolean> {
  private var evalFunCorrect: Boolean = true // only meaningful when inside an eval function or just came out of one

  override fun visitModuleFragment(declaration: IrModuleFragment, data: Boolean): IrModuleFragment {
    val foo = super.visitModuleFragment(declaration, data)
    println(declaration.dumpKotlinLike())
    return foo
  }
  override fun visitCall(expression: IrCall, insideEvalFun : Boolean): IrElement {
    val workingDir = Path("/homes/ef322/temp")
    // check if starts with eval
    evalFunCorrect = false
    val isEval = expression.symbol.owner.name.asString().startsWith("eval")
    val callerType = expression.dispatchReceiver?.type ?: expression.extensionReceiver?.type
    if (!(
        isEval || insideEvalFun
      ))
    {
      return expression
    }
    // check if caller and return type is a primitive
    if (!(expression.type.isPrimitiveType() && callerType != null && callerType.isPrimitiveType())) {
      return expression
    }
    // whether arguments are primitive
    // no generics are allowed
    // disallow varargs, and default values in eval functions since the type is not known at this point
    if (expression.typeArgumentsCount > 0 ||
      expression.valueArguments.all { it?.type?.isPrimitiveType() ?: false }
      ) {
        return expression
    }
    evalFunCorrect = true // passed all check in this level. Recurse down for further checks

    if (isEval) {// isEval == true
      expression.symbol.owner.accept(this, true)
      if (evalFunCorrect) {
        val fileName = "exec.kts"
        val evalScript = File(workingDir.name, fileName)
        evalScript.createNewFile()
        evalScript.writeText(expression.dumpKotlinLike())
        evalScript.appendText(expression.symbol.owner.dumpKotlinLike())
        val output = "kotlinc -script $fileName".runCommand(workingDir.toFile()) ?: return expression
        return when (expression.type) {
          pluginContext.irBuiltIns.intType -> IrConstImpl.int(0,0,expression.type, output.toInt())
          pluginContext.irBuiltIns.stringType -> IrConstImpl.string(0,0,expression.type, output)
          pluginContext.irBuiltIns.booleanType -> IrConstImpl.boolean(0,0,expression.type, output.toBoolean())
          // TODO Other primitves
          else -> expression
        }
      } else {
        return expression
      }
    } else { // inside an eval function
      super.visitCall(expression, insideEvalFun)
      return expression
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
