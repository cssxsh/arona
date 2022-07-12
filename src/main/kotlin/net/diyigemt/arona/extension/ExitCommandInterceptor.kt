package net.diyigemt.arona.extension

import net.diyigemt.arona.Arona
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.command.parse.CommandCall
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@OptIn(ExperimentalCommandDescriptors::class, ConsoleExperimentalApi::class)
object ExitCommandInterceptor: CommandInterceptor {
  private val EXIT_COMMAND = listOf(
    "${CommandManager.commandPrefix}stop",
    "${CommandManager.commandPrefix}shutdown",
    "${CommandManager.commandPrefix}exit"
  )
  override val level: Int = 1

  override fun interceptCall(call: CommandCall): Boolean {
    val calleeName = call.calleeName
    val valueArguments = call.valueArguments
    if (valueArguments.isEmpty() && EXIT_COMMAND.contains(calleeName)) {
      Arona.sendExitMessage()
    }
    return true;
  }

  override fun initInterceptor() {
    registerInterceptor()
  }
}