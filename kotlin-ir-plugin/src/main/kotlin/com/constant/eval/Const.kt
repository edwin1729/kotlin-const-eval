package com.constant.eval

sealed class Const {
  data class boolVal(val value: Boolean) : Const()
  data class intVal(val value: Int) : Const()
  data class stringVal(val value: String) : Const()
}
