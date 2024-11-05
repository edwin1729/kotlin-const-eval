# kotlin-const-eval

*This is a quick proof of concept style implementation. I've taken shortcuts in several places since I couldn't spend too much time on the task. I try to justify the shortcuts and this is not representative of anything I'd consider production ready*

My approach was to reuse the compiler to avoid redoing the work of evaluating `eval` functions. My goal is to be able to call compiler internals to compile just the bodies of the `eval` function. However that would require familiarizing myself with kotlin compiler internals so for this short task, I use the hack of going from kotlin IR to source code and compiling it with a subprocess

Specifically the kotlin IR  for an eval function call and its corresponding body is pretty printed to a koltin script file (.kts). I run this by spawning a subprocess and obtaining the output. The output is converted back to an `IrElement`. This is actually one of the problematic parts since I haven't yet figured out how to do this without manually mapping each `IrType` to a corresponding kotlin function for converting the string to a primitive(Int, Bool,etc). I'd like to explore reflection or any advanced features of the type system which may allow this.

I may have gone for a seemingly innefficient apprach, but I was drawn by the generality of it. I wanted to use the kotlin compiler itself to evaluate constants. This makes the approach easy to extend to all kinds of expressions. For example functions where the reciever type isn't a primitive, such as a user defined class may also be interpreted if only we also include the userdefined class in the compilation. In short the definition of a constant type may be extended easily. Another benefit is language constructs like lambdas (eg. the common `let` and `apply` lambdas) can be trivially accomodated.

I had planned to implement an optimization: use a Hashset mapping each eval function to a compiled executable so that it doesn't get recompiled (or rechecked for presence of non-primitive types in its definition) every single time. This would involve using a `.kt` file instead of `.kts` file having a main method which took in command line arguments which were infact arguments to the `eval` function, parse the string inputs to the right type and evaluate the eval function on these arguments. I would need to construct the main method using kotlin IR and this is quite a long process. As of now I haven't implemented the optimisation.

My current implementation sadly has the big limitation that the IR pretty printer `IrElement.dumpKotlinLike()` doesn't produce compilable code even for simple boolean comparison. However if we directly interface with compiler internals this would be a problem.

Finally I want to state that this fully meets requirements other than when the pretty printer doesn't print non-compilable code. This disallows basically any `if` statements.

## Extension! Nested Evals

Additionally another design decision of using a single transformer instead of a transformer for substituting eval function calls and visitors for checking presence of non-primitives, was made with an extension in mind. I can call eval functions from within eval functions and this requires this single transformer design.
Even nested calls to `eval` functions will get correctly substituted. There is an example in the [here](https://github.com/edwin1729/kotlin-const-eval/blob/main/kotlin-ir-plugin/src/test/kotlin/com/constant/eval/IrPluginTest.kt#L44)

## drawbacks of using IR pretty printer illustration
origianl code
```
fun evalDebug(): Boolean {
    if (5 == 6) {
        return true
    }
    return false
}
```
output of `dumpKotlinLike()`
```
fun evalDebug(): Boolean {
  when {
    EQEQ(arg0 = 5, arg1 = 6) -> { // BLOCK
      return true
    }
  }
  return false
}
```
