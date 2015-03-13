package com.lavsurgut.release.manager.lib.task


/**
 * @author Valery Lavrentiev, lavsurgut@gmail.com
 *
 */
abstract class Task {

	abstract void executeBeforeChecks()

	abstract void executeCommand()

	abstract void executeAfterChecks()


	void run () {
		executeBeforeChecks()
		executeCommand()
		executeAfterChecks()
	}
}
