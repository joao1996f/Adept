package adept.test.common

class Common {
  var programFileName = ""
  var firrtlArgs = Array[String]()
}

object Common {
  def apply(args: Array[String]): Common = {
    val common = new Common

    for ((name, i) <- args.zipWithIndex) {
      if (name.contains("--program-file=")) {
        common.programFileName = name.slice(15, name.length)
      } else {
        // FIRRTL args
        common.firrtlArgs = common.firrtlArgs :+ name
      }
    }

    println("Program to test is: " + common.programFileName)

    println("FIRRTL Arguments are: ")
    for ((arg, i) <- common.firrtlArgs.zipWithIndex) {
      println("\tArg(" + i + "): " + arg)
    }

    return common
  }
}
