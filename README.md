# kotlin-const-eval

status: has an identified bug. Please give me till Wednesday to fix it since I'm overloaded with coursework right now

I may have gone for a seemingly innefficient apprach, but I was drawn by the generality of it. I wanted to use the kotlin compiler itself to evaluate constants. This makes the appraoch very general and the plugin can be easily extended.

In this proof of concept this is acheived in an naive way. The kotlin IR  for an eval function call and its corresponding body is pretty printed to a koltin script file (.kts). I run this by spawining a subprocess and obtaining the output. The output is converted back to an `IrElement`. This is actually one of the problematic parts since I haven't yet figured out how to do this without manually mapping each `IrType` to a corresponding kotlin function for converting the string to a primitive(Int, Bool,etc)

I had plan to extend this by having a Hashset mapping each eval function to a compiled executable so that it doesn't get recompiled or rechecked for presence of non-primitive types in its definition.

Additionally another design decision of using a single transformer instead of a transformer for substituting eval function calls and visitors for checking presence of non-primitives, was made with an extension in mind. I wanted to be able to call eval functions from within eval functions for optimization reasons and this requires this single transformer design.
