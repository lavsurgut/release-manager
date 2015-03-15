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
		
		runnables.collect { r -> new Thread(new Runnable() {
			@Override
			void run() {
				log.info "Starting thread"		
				r.getValue().run()
				log.info "Finishing thread"
			}
		})}.each {t -> t.start()}.each {t -> t.join()}
		
	}



}
