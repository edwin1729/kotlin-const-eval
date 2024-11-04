//package com.constant.eval
//
//import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
//import org.jetbrains.kotlin.ir.declarations.IrFunction
//import org.jetbrains.kotlin.ir.expressions.IrBody
//import org.jetbrains.kotlin.ir.expressions.IrConstKind
//import org.jetbrains.kotlin.ir.types.IrType
//import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
//
//class ConstEvalVisitor(
//  private val pluginContext: IrPluginContext,
//) : IrElementVisitor<Const?, Nothing?> {
//  val evaluatedFns: Map<IrFunction, Const> = hashMapOf()
//
//  // manually check for the few primitive types considered
//  // TODO map all primitive kotlin types defined in `Const` automatically to IrType
//  private fun ifPrimitive(irType: IrType, eval: () -> Const?): Const? {
//    return if (irType == pluginContext.irBuiltIns.booleanType ||
//      irType == pluginContext.irBuiltIns.stringType ||
//      irType == pluginContext.irBuiltIns.intType
//    ) {
//      eval()
//    } else {
//      null
//    }
//  }
//
//  override fun visitFunction(fn: IrFunction, data: Nothing?): Const? =
//    ifPrimitive(fn.returnType) {
//      fn.body?.accept(this, data)
//    }
//
//  override fun visitBody(body: IrBody, data: Nothing?): Const? {
//    IrConstKind.String
//  }
//  override fun
//
//}
