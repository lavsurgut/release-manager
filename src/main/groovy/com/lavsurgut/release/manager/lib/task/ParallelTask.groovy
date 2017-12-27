/**
 * 
 */
package com.lavsurgut.release.manager.lib.task


import groovy.util.logging.Log4j

/**
 * @author Valery Lavrentiev, lavsurgut@gmail.com
 *
 */
@Log4j
class ParallelTask extends Task {

	HashMap<String, Task> runnables



	@Override
	public void executeCommand() {


		List<Throwable> caught	= new ArrayList<Throwable>()


		runnables.collect { r ->
			new Thread(new Runnable() {
						@Override
						void run() {
							log.info "Starting thread"

							r.getValue().run()

							log.info "Finishing thread"
						}
					})
		}.each {t ->
			t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
						public void uncaughtException(Thread th, Throwable ex) {
							caught.add(ex)
						}
					})
			t.start()
		}.each {t -> t.join()}
		//TODO: add proper merge of stack traces and send them?
		//now sends the first one found
		if (caught)
			throw new Exception(caught[0])
	}
}
